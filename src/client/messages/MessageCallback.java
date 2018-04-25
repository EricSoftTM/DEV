/*
@Author - Eric
* 
* MessageCallback Tutorial for Commands:
* Inside a command file, where you see int's, add public/private (your choice, ill go with private).
* private MessageCallback mc;
* when doing dropMessage, instead of player.dropmessage, do mc.dropMessage("");! alot easier! 
 */
package client.messages;

public interface MessageCallback {

    void dropMessage(String message);
    void dropMessage(int type, String message);
}
