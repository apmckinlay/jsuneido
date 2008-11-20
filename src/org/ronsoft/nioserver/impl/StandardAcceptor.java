package org.ronsoft.nioserver.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ronsoft.nioserver.Dispatcher;

/**
 * Created by IntelliJ IDEA.
 * User: ron
 * Date: Mar 17, 2007
 * Time: 6:07:38 PM
 */
public class StandardAcceptor
{
	private final Dispatcher dispatcher;
	private final InputHandlerFactory inputHandlerFactory;
	private final ServerSocketChannel listenSocket;
	private final Listener listener;
	private final List<Thread> threads = new ArrayList<Thread>();
	private final Logger logger = Logger.getLogger (getClass().getName());
	private volatile boolean running = true;

	public StandardAcceptor (ServerSocketChannel listenSocket, Dispatcher dispatcher,
		InputHandlerFactory inputHandlerFactory)
	{
		this.listenSocket = listenSocket;
		this.dispatcher = dispatcher;
		this.inputHandlerFactory = inputHandlerFactory;

		listener = new Listener();
	}

	public StandardAcceptor (SocketAddress socketAddress, Dispatcher dispatcher,
		InputHandlerFactory inputHandlerFactory)
		throws IOException
	{
		this (ServerSocketChannel.open(), dispatcher, inputHandlerFactory);

		listenSocket.socket().bind (socketAddress);
	}

	public StandardAcceptor (int port, Dispatcher dispatcher,
		InputHandlerFactory inputHandlerFactory)
		throws IOException
	{
		this (new InetSocketAddress (port), dispatcher, inputHandlerFactory);
	}


	private class Listener implements Runnable
	{
		public void run ()
		{
			while (running) {
				try {
					SocketChannel client = listenSocket.accept();
					// prevent Nagle/Ack delays
					// might be better to use scatter/gather writes
					client.socket().setTcpNoDelay(true);
					if (client == null) {
						continue;
					}

					dispatcher.registerChannel (client, inputHandlerFactory.newHandler());

				} catch (ClosedByInterruptException e) {
					logger.fine ("ServerSocketChannel closed by interrupt: " + e);
					return;

				} catch (ClosedChannelException e) {
					logger.log (Level.SEVERE,
						"Exiting, serverSocketChannel is closed: " + e, e);
					return;

				} catch (Throwable t) {
					logger.log (Level.SEVERE,
						"Exiting, Unexpected Throwable doing accept: " + t, t);

					try {
						listenSocket.close();
					} catch (Throwable e1) { /* nothing */ }

					return;
				}
			}
		}
	}

	public synchronized Thread newThread()
	{
		Thread thread = new Thread (listener);

		threads.add (thread);

		thread.start();

		return thread;
	}

	public synchronized void shutdown()
	{
		running = false;

		for (Iterator<Thread> it = threads.iterator(); it.hasNext();) {
			Thread thread = it.next();

			if ((thread != null) && (thread.isAlive())) {
				thread.interrupt();
			}
		}

		for (Iterator<Thread> it = threads.iterator(); it.hasNext();) {
			Thread thread = it.next();

			try {
				thread.join();
			} catch (InterruptedException e) {
				// nothing
			}

			it.remove();
		}

		try {
			listenSocket.close();
		} catch (IOException e) {
			logger.log (Level.SEVERE, "Caught an exception shutting down", e);
		}
	}
}
