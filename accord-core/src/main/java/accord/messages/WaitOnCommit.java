package accord.messages;

import java.util.List;
import java.util.stream.Collectors;

import accord.local.Instance;
import accord.local.Node;
import accord.local.Node.Id;
import accord.messages.Reply;
import accord.messages.Request;
import accord.local.Command;
import accord.local.Listener;
import accord.txn.TxnId;
import accord.txn.Keys;

public class WaitOnCommit implements Request
{
    static class LocalWait implements Listener
    {
        final Node node;
        final Id replyToNode;
        final long replyToMessage;

        int waitingOn;

        LocalWait(Node node, Id replyToNode, long replyToMessage)
        {
            this.node = node;
            this.replyToNode = replyToNode;
            this.replyToMessage = replyToMessage;
        }

        @Override
        public synchronized void onChange(Command command)
        {
            switch (command.status())
            {
                default:
                    throw new IllegalStateException();
                case NotWitnessed:
                case PreAccepted:
                case Accepted:
                    return;

                case Committed:
                case Executed:
                case Applied:
                case ReadyToExecute:
            }

            command.removeListener(this);
            ack();
        }

        private void ack()
        {
            if (--waitingOn == 0)
                node.reply(replyToNode, replyToMessage, new WaitOnCommitOk());
        }

        synchronized void setup(TxnId txnId, Keys keys)
        {
            List<Instance> instances = node.local(keys).collect(Collectors.toList());
            waitingOn = instances.size();
            instances.forEach(instance -> {
                Command command = instance.command(txnId);
                switch (command.status())
                {
                    case NotWitnessed:
                    case PreAccepted:
                    case Accepted:
                        command.addListener(this);
                        break;

                    case Committed:
                    case Executed:
                    case Applied:
                    case ReadyToExecute:
                        ack();
                }
            });
        }
    }

    final TxnId txnId;
    final Keys keys;

    public WaitOnCommit(TxnId txnId, Keys keys)
    {
        this.txnId = txnId;
        this.keys = keys;
    }

    public void process(Node node, Id replyToNode, long replyToMessage)
    {
        new LocalWait(node, replyToNode, replyToMessage).setup(txnId, keys);
    }

    public static class WaitOnCommitOk implements Reply
    {
    }
}
