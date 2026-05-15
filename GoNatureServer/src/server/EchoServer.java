package server;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import firstPackage.Message;
import firstPackage.Order;
import java.util.ArrayList;

public class EchoServer extends AbstractServer {

	public EchoServer(int port) {
		super(port);
	}

	// ---------------------------------------------------------
	// 1. This method handles messages coming FROM the Client
	// ---------------------------------------------------------
	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		System.out.println("Message received: " + msg + " from " + client);

		if (msg instanceof Message) {
			Message request = (Message) msg;

			try {
				// If the client wants to load the table
				if (request.getCommand().equals("GET_ORDERS")) {
					System.out.println("Client requested orders.");

					// MAKE SURE THESE TWO LINES ARE NOT COMMENTED OUT (No // in front of them)
					ArrayList<Order> orders = DBController.getOrders();
					client.sendToClient(new Message("ORDERS_DATA", orders));
				}

				// If the client wants to update a row
				else if (request.getCommand().equals("UPDATE_ORDER")) {
					Order orderToUpdate = (Order) request.getData();
					System.out.println("Client requested to update order: " + orderToUpdate.getOrderNumber());

					// MAKE SURE THESE TWO LINES ARE NOT COMMENTED OUT
					DBController.updateOrder(orderToUpdate);
					client.sendToClient(new Message("UPDATE_SUCCESS", null));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// ---------------------------------------------------------
	// 2. REQUIRED BY ASSIGNMENT: Show connected client IP & Host
	// ---------------------------------------------------------
	@Override
	protected void clientConnected(ConnectionToClient client) {
		String ip = client.getInetAddress().getHostAddress();
		String host = client.getInetAddress().getHostName();
		System.out.println("Client Connected!");
		System.out.println("IP: " + ip);
		System.out.println("Host: " + host);
	}

	@Override
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	@Override
	protected void serverStopped() {
		System.out.println("Server has stopped listening for connections.");
	}
}