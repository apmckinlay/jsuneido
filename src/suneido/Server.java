package suneido;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.io.*;

/**
 * See:<ul>
 * <li>TCP/IP Sockets in Java 2nd Ed.
 * <li><a href="http://javanio.info/filearea/nioserver/NIOServerMark2.pdf">
 *		How to Build a Scalable Multiplexed Server with NIO Mark II</a>
 * <li><a href="http://today.java.net/cs/user/print/a/350">
 * 		Architecture of a Highly Scalable NIO-Based Server</a>
 * </ul>
 * <b>NOTE: This version is single threaded.</b>
 * @author Andrew McKinlay
 */
public class Server {
	static final int PORT = 3456;
	
	public static void main(String[] args) throws IOException {
		run(PORT);
	}
	
	static Selector selector;
	
	public static void run(int port) throws IOException {
		selector = Selector.open();
		ServerSocketChannel listenChannel = ServerSocketChannel.open();
		// to avoid problems restarting server
		// as recommended by Effective TCP/IP Programming
		listenChannel.socket().setReuseAddress(true);
		listenChannel.socket().bind(new InetSocketAddress(port));
		listenChannel.configureBlocking(false);
		listenChannel.register(selector, SelectionKey.OP_ACCEPT);		
		while (true) {
			selector.select();
			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				if (key.isAcceptable())
					handleAccept(key);
				else if (key.isReadable())
					handleRead(key);
				else if (key.isWritable())
					handleWrite(key);
				keyIter.remove();
			}
		}
	}
	
	public static void handleAccept(SelectionKey key) throws IOException {
		SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
		channel.configureBlocking(false);
		channel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1000));
	}

	public static void handleRead(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buf = (ByteBuffer) key.attachment();
		int bytesRead = channel.read(buf);
		if (bytesRead == -1)
			channel.close();
		else if (bytesRead > 0 && requestComplete(buf)) {
			handleRequest(key, buf);
		}
	}
	
	public static void handleRequest(SelectionKey key, ByteBuffer buf) throws IOException {
		//TODO just echo for now
		buf.flip();
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private static boolean requestComplete(ByteBuffer buf) {
		return true; //TODO check for newline
	}

	private static void handleWrite(SelectionKey key) throws IOException {
		ByteBuffer buf = (ByteBuffer) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();
		channel.write(buf);
		if (buf.hasRemaining())
			return ;
		buf.clear();
		key.interestOps(SelectionKey.OP_READ);
	}

}
