package no.hvl.dat110.broker;

import java.util.Set;
import java.util.Collection;

import no.hvl.dat110.common.Logger;
import no.hvl.dat110.common.Stopable;
import no.hvl.dat110.messages.*;
import no.hvl.dat110.messagetransport.Connection;

public class Dispatcher extends Stopable {

    private Storage storage;

    public Dispatcher(Storage storage) {
        super("Dispatcher");
        this.storage = storage;

    }

    @Override
    public void doProcess() {

        Collection<ClientSession> clients = storage.getSessions();

        Logger.lg(".");
        for (ClientSession client : clients) {

            Message msg = null;

            if (client.hasData()) {
                msg = client.receive();
            }

            if (msg != null) {
                dispatch(client, msg);
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void dispatch(ClientSession client, Message msg) {

        MessageType type = msg.getType();

        switch (type) {

            case DISCONNECT:
                onDisconnect((DisconnectMsg) msg);
                break;

            case CREATETOPIC:
                onCreateTopic((CreateTopicMsg) msg);
                break;

            case DELETETOPIC:
                onDeleteTopic((DeleteTopicMsg) msg);
                break;

            case SUBSCRIBE:
                onSubscribe((SubscribeMsg) msg);
                break;

            case UNSUBSCRIBE:
                onUnsubscribe((UnsubscribeMsg) msg);
                break;

            case PUBLISH:
                onPublish((PublishMsg) msg);
                break;

            default:
                Logger.log("broker dispatch - unhandled message type");
                break;

        }
    }

    // called from Broker after having established the underlying connection
    public void onConnect(ConnectMsg msg, Connection connection) {

        String user = msg.getUser();

        Logger.log("onConnect:" + msg.toString());

        try{
            storage.addClientSession(user, connection);
            if (storage.getDisconnectedClients().containsKey(user)) {
                for (String id : storage.getDisconnectedClients().get(user)) {
                    MessageUtils.send(connection, storage.bufferedMessages.get(id));
                    Logger.log("sending unread message to " + user);
                    storage.bufferedMessages.remove(id);
                }
                // fjerner bruker fra disconected List når brukerern connecter igjen
                Logger.log("removing " + user + " from the disconnected list");
                storage.disconnectedClients.remove(user);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
            Logger.log("Error");
        }
        //DONE: how to handle user connection

    }

    // called by dispatch upon receiving a disconnect message
    public void onDisconnect(DisconnectMsg msg) {

        String user = msg.getUser();

        Logger.log("onDisconnect:" + msg.toString());
        try{
            storage.removeClientSession(user);
            storage.addToDisconnected(user);
        }catch (NullPointerException e){
            e.printStackTrace();
            Logger.log("Error");
        }
        //DONE: on disconected user
    }

    public void onCreateTopic(CreateTopicMsg msg) {

        Logger.log("onCreateTopic:" + msg.toString());
        try {
            storage.createTopic(msg.getTopic());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Logger.log("error");
        }
        // DONE: create the topic in the broker storag

    }

    public void onDeleteTopic(DeleteTopicMsg msg) {

        Logger.log("onDeleteTopic:" + msg.toString());

        try {
            storage.deleteTopic(msg.getTopic());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Logger.log("error");
        }

        // DONE: delete the topic from the broker storage
    }

    public void onSubscribe(SubscribeMsg msg) {

        Logger.log("onSubscribe:" + msg.toString());
        try {
            storage.addSubscriber(msg.getUser(), msg.getSubscribeTo());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Logger.log("error");
        }
        // DONE: subscribe user to the topic
    }

    public void onUnsubscribe(UnsubscribeMsg msg) {

        Logger.log("onUnsubscribe:" + msg.toString());
        try {
            storage.removeSubscriber(msg.getUser(), msg.getUnSubTo());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Logger.log("error");
        }
        // DONE: unsubscribe user to the topic

    }

    public void onPublish(PublishMsg msg) {
    // DONE: publish the message to clients subscribed to the topic
        Logger.log("onPublish:" + msg.toString());
        try {
            Collection<ClientSession> clients = storage.getSessions();
            for (ClientSession client : clients) {
                if (storage.subscriptions.get(msg.getTopic()).contains(client.getUser())) {
                    MessageUtils.send(client.getConnection(), msg);
                }
            }
            for (String subbedUsers : storage.getSubscribers(msg.getTopic())) {
                if (storage.disconnectedClients.containsKey(subbedUsers)) {
                    storage.addToBufferAndToUnread(msg.getTopic(), msg, subbedUsers);
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Logger.log("error");
        }
    }

}
