package com.cloudbility.rtunnel.buffer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.common.crypto.AESCipher;
import com.cloudbility.common.crypto.CryptoException;
import com.skybility.cloudsoft.rtunnel.common.Compression;
import com.skybility.cloudsoft.rtunnel.common.CompressionImpl;

/**
 * 一个可重用IO包的封装，包括缓冲区、压缩/解压、加密/解密。 数据缓冲区的长度不会超过 {@link #PACKET_MAX_SIZE}
 * ，支持动态自动扩容。 不支持并发操作。
 * 
 * @author atlas
 * @date 2012-9-19
 */
public class Packet {
	private static final Logger logger = LoggerFactory.getLogger(Packet.class);

	// max packet size 256KB
	public static final int PACKET_MAX_SIZE = 256 * 1024;

	/**
	 * packet head size,also data header offset
	 */
	public static int HEAD_SIZE = 5;
	/**
	 * [5 bytes for head : data bytes : unused bytes]
	 * 
	 */
	private byte[] buffer;
	/**
	 * index of the data tailer offset in buffer
	 */
	private int index = HEAD_SIZE;
	/**
	 * reading index in the buffer
	 */
	private int readIndex = HEAD_SIZE;
	/**
	 * protocol type, 1 byte
	 */
	protected int type;

	/**
	 * AES128加密以及数据头增加的长度16,协议头 5,压缩增加的 32
	 */
	private final int buffer_margin = 16 // AES 128 max padding size.
	+ HEAD_SIZE // protocol header.
	+ 32; // margin for deflater; deflater may inflate data

	// 心跳
	public static final int HEART_BEAT = 0x00;
	// ACK心跳
	public static final int ACK_HEART_BEAT = 0x01;
	// Forward-server
	public static final int TCP_SERVER_PORT = 0x02;
	// ACk Forward-server
	public static final int ACK_TCP_SERVER_PORT = 0x03;
	// new Forward-client
	public static final int NEW_TCP_SOCKET = 0x04;
	// ack-new Forward-client
	public static final int ACK_NEW_TCP_SOCKET = 0x05;
	// data
	public static final int DATA = 0x06;
	// close tunnel
	public static final int CLOSE_TUNNEL = 0x07;

	// 当前的协议以及用于扩展协议占用的bit位的MASK，即当前有7种协议0x01-0x07,预留0x08-0x0F,占用低4bit
	public static final int PROTOCOL_BIT_MASK = 0x0F;
	// type&=CLEAR_PROTOCOL_BIT_MASK 可以清空协议
	// type|=PROTOCOL可以设置协议类型
	public static final int CLEAR_PROTOCOL_BIT_MASK = 0xFF - PROTOCOL_BIT_MASK;

	public void setProtocol(int protocol) {
		if (protocol < 0 || protocol > PROTOCOL_BIT_MASK) {
			throw new IllegalArgumentException("Unsupported protocol "
					+ protocol + ",protocol type must be [0,"
					+ PROTOCOL_BIT_MASK + "]");
		}
		// clean previous protocol
		this.type &= CLEAR_PROTOCOL_BIT_MASK;
		this.type |= protocol;
		this.buffer[0] = (byte) this.type;
	}

	public boolean isProtocol(int protocol) {
		if (protocol < 0 || protocol > PROTOCOL_BIT_MASK) {
			throw new IllegalArgumentException("Unsupported protocol "
					+ protocol + ",protocol type must be [0,"
					+ PROTOCOL_BIT_MASK + "]");
		}
		return (this.type & PROTOCOL_BIT_MASK) == protocol;
	}

	/*
	 * 高位 2 bit 用来表示压缩 和 加密标识，用位掩码表示，相容
	 */
	// 1000 0000
	public static final int COMPRESSED = 0x80;
	// 0100 0000
	public static final int ENCRYPTED = 0x40;
	// 1100 0000 高2bit的MASK
	public static final int HIGH_MASK = 0xC0;

	private static Map<Integer, String> SEG_TYPE_DESC = new HashMap<Integer, String>();
	static {
		SEG_TYPE_DESC.put(HEART_BEAT, "HEART_BEAT_PACKET");
		SEG_TYPE_DESC.put(ACK_HEART_BEAT, "ACK_HEART_BEAT");
		SEG_TYPE_DESC.put(TCP_SERVER_PORT, "TCP_SERVER_PORT");
		SEG_TYPE_DESC.put(ACK_TCP_SERVER_PORT, "ACK_TCP_SERVER_PORT");
		SEG_TYPE_DESC.put(NEW_TCP_SOCKET, "NEW_TCP_SOCKET");
		SEG_TYPE_DESC.put(ACK_NEW_TCP_SOCKET, "ACK_NEW_TCP_SOCKET");
		SEG_TYPE_DESC.put(DATA, "DATA_PACKET");
		SEG_TYPE_DESC.put(CLOSE_TUNNEL, "CLOSE_TUNNEL");
	}

	/**
	 * 
	 * @param size
	 *            初始buffer size
	 */
	public Packet(int size) {
		if (size >= PACKET_MAX_SIZE) {
			throw new IllegalArgumentException("packet size " + size
					+ " larger than " + PACKET_MAX_SIZE);
		}
		if (size < 0) {
			throw new IllegalArgumentException("packet size " + size
					+ " must not be negtive.");
		}
		this.buffer = new byte[size + buffer_margin];
		init(6);
	}

	public Packet(Packet packet) {
		packet.fillHeader();
		buffer = packet.buffer;
		deflater = packet.deflater;
		inflater = packet.inflater;
		cipher = packet.cipher;
		type = packet.type;
		index = packet.index;
		readIndex = packet.readIndex;
	}

	/**
	 * 
	 * @param size
	 * @param key
	 */
	public Packet(int size, byte[] key) throws CryptoException {
		this(size);
		cipher = AESCipher.getInstance(key);
	}

	public Packet(int size, AESCipher cipher) {
		this(size);
		this.cipher = cipher;
	}

	/**
	 * for compressing
	 */
	private CompressionImpl deflater;
	/**
	 * for uncompressing
	 */
	private CompressionImpl inflater;
	/**
	 * 
	 */
	private AESCipher cipher;

	private void init(int level) {
		deflater = new CompressionImpl(buffer_margin);
		deflater.init(Compression.DEFLATER, level);
		inflater = new CompressionImpl(buffer_margin);
		inflater.init(Compression.INFLATER, level);
	}

	/**
	 * 一个byte的协议头
	 * 
	 * @return
	 */
	public int getType() {
		return type;
	}

	/**
	 * 
	 * @return true if this packet's data is compressed.
	 */
	public boolean isCompressed() {
		return (type & COMPRESSED) != 0;
	}

	/**
	 * set to compress this packet's data when invoke {@link #encode()} method
	 */
	public void setCompressed() {
		type |= COMPRESSED;
	}

	/**
	 * 
	 * @return true if this packet's data is encrypted.
	 */
	public boolean isEncrypted() {
		return (type & ENCRYPTED) != 0;
	}

	/**
	 * set to encrypt this packet's data when invoke {@link #encode()} method
	 */
	public void setEncrypted() {
		type |= ENCRYPTED;
	}

	/**
	 * set not to encrypt this packet's data when invoke {@link #encode()}
	 * method
	 */
	public void clearEncrypted() {
		type &= 0xBF;
	}

	/**
	 * set not to compress this packet's data when invoke {@link #encode()}
	 * method
	 */
	public void clearCompressed() {
		type &= 0x7F;
	}

	/**
	 * clear header information
	 */
	public void clearHeader() {
		type = 0;
		byte zero = 0;
		for (int i = 0; i < HEAD_SIZE; i++) {
			this.buffer[i] = zero;
		}
	}

	/**
	 * 
	 * @return true if this is a HEART_BEAT packet
	 */
	// type & 0011
	public boolean isHeartBeat() {
		return isProtocol(HEART_BEAT);
	}

	/**
	 * set this packet for HEART_BEAT
	 */
	public void setHeartBeat() {
		setProtocol(HEART_BEAT);
	}

	/**
	 * 
	 * @return true if this is a ACK_HEART_BEAT packet
	 */
	// type & 0011
	public boolean isAckHeartBeat() {
		return isProtocol(ACK_HEART_BEAT);
	}

	/**
	 * set this packet for ACK_HEART_BEAT
	 */
	public void setAckHeartBeart() {
		setProtocol(ACK_HEART_BEAT);
	}

	/**
	 * clear packet body
	 */
	public void clearBody() {
		index = HEAD_SIZE;
		readIndex = HEAD_SIZE;
	}

	/**
	 * clear this packet for reusing.
	 */
	public void clear() {
		clearHeader();
		clearBody();
	}

	/**
	 * extract bytes from this buffer to a byte array,increasing readIndex
	 * 
	 * @param dest
	 * @param start
	 * @param len
	 * @return data length actually read or 0 if no bytes available.
	 */
	public int extractBytes(byte[] dest, int start, int len) {
		int rlen = Math.min(index - readIndex, len);
		if (rlen > 0) {
			System.arraycopy(buffer, readIndex, dest, start, rlen);
			readIndex += rlen;
			return rlen;
		} else {
			return 0;
		}
	}

	/**
	 * extract bytes from this buffer to a byte array,increasing readIndex
	 * 
	 * @see #extractBytes(byte[], int, int)
	 * @param dest
	 * @return data length actually read or 0 if no bytes available.
	 */
	public int extractBytes(byte[] dest) {
		return extractBytes(dest, 0, dest.length);
	}

	/**
	 * feed bytes from src to this buffer,increasing index
	 * 
	 * @param src
	 * @param start
	 * @param len
	 */
	public void feedBytes(byte[] src, int start, int len) {
		// TODO buffer full check.
		ensureSize((buffer.length - index) + len);
		System.arraycopy(src, start, buffer, index, len);
		index += len;
	}

	/**
	 * 把一个byte数组填入数据区
	 * 
	 * @see #feedBytes(byte[], int, int)
	 * @param src
	 */
	public void feedBytes(byte[] src) {
		feedBytes(src, 0, src.length);
	}

	/**
	 * 把一个long整数填入数据区
	 * 
	 * @param v
	 */
	public void feedLong(long v) {
		ensureSize(getDataLen() + 8);
		buffer[index++] = (byte) (v >>> 56);
		buffer[index++] = (byte) (v >>> 48);
		buffer[index++] = (byte) (v >>> 40);
		buffer[index++] = (byte) (v >>> 32);
		buffer[index++] = (byte) (v >>> 24);
		buffer[index++] = (byte) (v >>> 16);
		buffer[index++] = (byte) (v >>> 8);
		buffer[index++] = (byte) (v >>> 0);
	}

	/**
	 * 从数据区抽出一个long（8 bytes），增加readIndex
	 * 
	 * @return
	 * @throws IOException
	 *             如果数据区的长度小于8
	 */
	public long extractLong() throws IOException {
		if (index - readIndex < 8)
			throw new IOException("Insufficient data for long");
		return (long) ((long) (0xff & buffer[readIndex++]) << 56
				| (long) (0xff & buffer[readIndex++]) << 48
				| (long) (0xff & buffer[readIndex++]) << 40
				| (long) (0xff & buffer[readIndex++]) << 32
				| (long) (0xff & buffer[readIndex++]) << 24
				| (long) (0xff & buffer[readIndex++]) << 16
				| (long) (0xff & buffer[readIndex++]) << 8 | (long) (0xff & buffer[readIndex++]) << 0);
	}

	/**
	 * 从数据区抽出一个int（4 bytes），增加readIndex
	 * 
	 * @return
	 * @throws IOException
	 *             如果数据区的长度小于4
	 */
	public int extractInt() throws IOException {
		if (index - readIndex < 4)
			throw new IOException("Insufficient data for int");
		return (int) (((buffer[readIndex++] & 0xff) << 24)
				| ((buffer[readIndex++] & 0xff) << 16)
				| ((buffer[readIndex++] & 0xff) << 8) | (buffer[readIndex++] & 0xff));
	}

	/**
	 * 把一个int整数填入数据区，增加index
	 * 
	 * @param v
	 */
	public void feedInt(int v) {
		ensureSize(getDataLen() + 4);
		buffer[index++] = (byte) (v >>> 24);
		buffer[index++] = (byte) (v >>> 16);
		buffer[index++] = (byte) (v >>> 8);
		buffer[index++] = (byte) (v >>> 0);
	}

	/**
	 * test use
	 * 
	 * @return
	 */
	public byte[] toBytes() {
		byte[] bytes = new byte[index - HEAD_SIZE];
		System.arraycopy(buffer, HEAD_SIZE, bytes, 0, bytes.length);
		return bytes;
	}

	/**
	 * 数据区特定位置上的一个byte的数据
	 * 
	 * @param index
	 * @return
	 */
	public int byteAt(int index) {
		return buffer[HEAD_SIZE + index];
	}

	/**
	 * 根据设置的标识位进行压缩、加密等等
	 */
	public void encode() {
		// 先压缩后加密
		if (isCompressed()) {
			compress();
		}
		if (isEncrypted()) {
			encrypt();
		}
	}

	/**
	 * 根据标识位进行解密和解压
	 */
	public void decode() {
		// 先解密后解压
		if (isEncrypted()) {
			decrypt();
			clearEncrypted();
		}
		if (isCompressed()) {
			uncompress();
			clearCompressed();
		}
	}

	public void fillHeader() {
		int len = index - HEAD_SIZE;
		buffer[0] = (byte) type;
		buffer[1] = (byte) (len >>> 24);
		buffer[2] = (byte) (len >>> 16);
		buffer[3] = (byte) (len >>> 8);
		buffer[4] = (byte) (len);
	}

	public void readHeader() {
		this.type = buffer[0];
		int len = ((buffer[1] << 24) & 0xff000000)
				| ((buffer[2] << 16) & 0x00ff0000)
				| ((buffer[3] << 8) & 0x0000ff00) | ((buffer[4]) & 0x000000ff);
		index = len + HEAD_SIZE;
	}

	/**
	 * resize this packet's buffer to size.
	 * 
	 * @param size
	 */
	private void resize(int size) {
		this.buffer = Arrays.copyOf(this.buffer, size);
	}

	/**
	 * ensure size for packet buffer
	 * 
	 * @param size
	 */
	public void ensureSize(int size) {
		// TODO check buffer full,i.e. reach PACKET_MAX_SIZE
		if (this.buffer.length < size + buffer_margin) {
			int resize = Math.min(PACKET_MAX_SIZE + buffer_margin,
					Math.max(buffer.length << 1, size + buffer_margin));
			if (resize < size) {
				throw new IllegalArgumentException("packet size " + size
						+ ",exceed maximum packet size is " + PACKET_MAX_SIZE);
			}
			resize(resize);
		}
	}

	/**
	 * 
	 * @return effective data length in this packet,not include head
	 *         information.
	 */
	public int getDataLen() {
		return index - HEAD_SIZE;
	}

	public void setDataLen(int len) {
		index = len + HEAD_SIZE;
	}

	/**
	 * compress data from HEAD_SIZE to index
	 */
	private void compress() {
		int[] len = new int[1];
		len[0] = index - HEAD_SIZE;
		buffer = deflater.compress(buffer, HEAD_SIZE, len);
		index = len[0] + HEAD_SIZE;
	}

	/**
	 * encrypt data from HEAD_SIZE to index
	 */
	private void encrypt() {
		if (cipher != null) {
			int padding = cipher.pad(getDataLen());
			ensureSize(padding + index);
			try {
				int l = cipher.encrypt(buffer, HEAD_SIZE, getDataLen(), buffer,
						HEAD_SIZE);
				if (l == 0) {
					// encryption fail
					clearEncrypted();
				} else {
					index += padding;
				}
			} catch (CryptoException e) {
				throw new RuntimeException("encryption failed", e);
			}
		} else {
			// cipher not set ,do not encrypt it.
			logger.warn("Cipher not set,unable to encrypt");
			clearEncrypted();
		}
	}

	/**
	 * decrypt data from HEAD_SIZE to index
	 */
	private void decrypt() {
		if (cipher != null) {
			try {
				// len: actual data length
				int len = cipher.decrypt(buffer, HEAD_SIZE, getDataLen(),
						buffer, HEAD_SIZE);
				index = HEAD_SIZE + len;
			} catch (CryptoException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		} else {
			throw new RuntimeException("AESChiper not set,unable to decrypt.");
		}
	}

	/**
	 * uncompress data from HEAD_SIZE to index
	 */
	private void uncompress() {
		int[] len = new int[1];
		len[0] = index - HEAD_SIZE;
		buffer = inflater.uncompress(buffer, Packet.HEAD_SIZE, len);
		index = len[0] + HEAD_SIZE;
	}

	@Override
	public String toString() {
		if (DEBUG)
			return toDebugString();
		return "Packet{compressed=" + isCompressed() + ",encrypted="
				+ isEncrypted() + ",type=" + getTypeDesc() + ",datalen="
				+ getDataLen() + "}";

	}

	public static boolean DEBUG = false;

	public String toDebugString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = HEAD_SIZE; i < index; i++) {
			sb.append(buffer[i]);
			sb.append(",");
		}
		if (getDataLen() > 0) {
			sb.setLength(sb.length() - 1);
		}
		sb.append("]");
		return "Packet{compressed=" + isCompressed() + ",encrypted="
				+ isEncrypted() + ",type=" + getTypeDesc() + ",datalen="
				+ getDataLen() + ",data=" + sb + "}";
	}

	protected String getTypeDesc() {
		String type = SEG_TYPE_DESC.get(this.type & PROTOCOL_BIT_MASK);
		return type != null ? type : "Unknown type"
				+ Integer.toString(this.type, 2) + "b";
	}

	public byte[] getBuffer() {
		fillHeader();
		return buffer;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * 将ChannelBuffer的长度为length的内容当成一个完整的Packet读入当前包的头部和数据区。
	 * 注意ChannelBuffer可能读到了下一个包的数据
	 * 
	 * @param in
	 * @param length
	 *            包的长度（包括HEAD_SIZE）
	 * @throws IOException
	 */
	public void readPacket(ChannelBuffer in, int length) throws IOException {
		ensureSize(length - HEAD_SIZE);
		in.readBytes(buffer, 0, length);
		readHeader();
	}

	/**
	 * 将整个Packet包装成ChannelBuffer,zero copy
	 * 
	 * @return
	 */
	public ChannelBuffer wrapPacket() {
		fillHeader();
		return ChannelBuffers.wrappedBuffer(this.buffer, 0, index);
	}

	/**
	 * 将包的数据区包装成ChannelBuffer,zero copy
	 * 
	 * @return
	 */
	public ChannelBuffer wrapPacketData() {
		return ChannelBuffers.wrappedBuffer(this.buffer, HEAD_SIZE, index
				- HEAD_SIZE);
	}

	/**
	 * 将ChannelBuffer的所有内容读到packet的数据区
	 * 
	 * @param in
	 * @throws IOException
	 */
	public void readData(ChannelBuffer in) throws IOException {
		int len = in.readableBytes();
		ensureSize(len);
		in.readBytes(buffer, HEAD_SIZE, len);
		index += len;
	}

	public static Packet fillTcpServerPortPacket(Packet p, int tcpPort,
			int rTcpPort) {
		p.clear();
		p.setProtocol(Packet.TCP_SERVER_PORT);
		p.feedInt(tcpPort);
		p.feedInt(rTcpPort);
		return p;
	}

	public static Packet fillACKTcpServerPortPacket(Packet p, int result) {
		byte[] resultBytes = new byte[] { (byte) (result >>> 0) };
		setControlPacket(p, Packet.ACK_TCP_SERVER_PORT, resultBytes);
		return p;
	}

	static void setControlPacket(Packet p, int type, byte[] resultBytes) {
		p.clear();
		p.setProtocol(type);
		p.feedBytes(resultBytes);
	}

	static void setControlPacket(Packet p, int type, int intResult) {
		p.clear();
		p.setProtocol(type);
		p.feedInt(intResult);
	}

	static void setControlPacket(Packet p, int type, long longResult) {
		p.clear();
		p.setProtocol(type);
		p.feedLong(longResult);
	}

	public static Packet fillNewTcpSocketPacket(Packet p, int serverSockBindInfo) {
		setControlPacket(p, Packet.NEW_TCP_SOCKET, serverSockBindInfo);
		return p;
	}

	public static Packet fillACKNewTcpSocketPacket(Packet p,
			int serverSockBindInfo) {
		setControlPacket(p, Packet.ACK_NEW_TCP_SOCKET, serverSockBindInfo);
		return p;
	}

	public static Packet fillHeartBeatPacket(Packet p) {
		setControlPacket(p, Packet.HEART_BEAT, System.currentTimeMillis());
		return p;
	}

	public static Packet fillACKHeartBeatPacket(Packet p, byte[] timeBytes) {
		setControlPacket(p, Packet.ACK_HEART_BEAT, timeBytes);
		return p;
	}

	public static Packet fillCloseTunnelPacket(Packet p) {
		byte[] resultBytes = new byte[0];
		setControlPacket(p, Packet.CLOSE_TUNNEL, resultBytes);
		return p;
	}

}
