package ch.fhnw.jsamp;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;

import java.util.Map;

public class IDL_MessageHandler extends AbstractMessageHandler {
    private Call latestCall = null;

    public IDL_MessageHandler(String[] mtypes) {
        super(mtypes);
    }

    @Override
    public Map processCall(HubConnection hubConnection, String s, Message message) throws Exception {
        latestCall = new Call(hubConnection, s, message);
        return null; //new Response(new Message(message.getMType()));
    }

    class Call {
        private HubConnection hubConnection;
        private String s;
        private Message message;
        protected Call(HubConnection hubConnection, String s, Message message) {
            this.hubConnection = hubConnection;
            this.s = s;
            this.message = message;
        }

        public HubConnection getHubConnection() {
            return hubConnection;
        }

        public String getS() {
            return s;
        }

        public Message getMessage() {
            return message;
        }
    }

    public Call getLatestCall() {
        Call c = this.latestCall;
        latestCall = null;
        return c;
    }
}
