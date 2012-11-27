/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
 */
package com.cloudbility.rtunnel.common;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.common.crypto.AESCipher;
import com.cloudbility.common.skip.SKIP;
import com.cloudbility.common.skip.SkipException;
import com.cloudbility.rtunnel.buffer.Packet;
import com.cloudbility.rtunnel.client.TunnelConfig;

public class DHKeyExchangerHandler extends CommonHandler {

	private static Logger logger = LoggerFactory.getLogger(DHKeyExchangerHandler.class);

	// shared if DH key exchanger is finished
	private boolean shared = false;

	private KeyPair dhKeyPair;

	private PublicKey peerDHPublicKey;

	private CipherHolder cipherHolder;

//	private AESCipher cipher;

	private void init() throws SkipException {
		try {
			// Create a Diffie-Hellman key pair.
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
			kpg.initialize(SKIP.DHParameterSpec);
			dhKeyPair = kpg.genKeyPair();
		} catch (InvalidAlgorithmParameterException e) {
			throw new SkipException("Invalid DH algorithm parameter.", e);
		} catch (NoSuchAlgorithmException e) {
			throw new SkipException("DH algorithm not supported.", e);
		}
	}
	
	public DHKeyExchangerHandler(CipherHolder cipherHolder) throws SkipException {
		this.init();
		this.cipherHolder = cipherHolder;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (!shared) {
			sendDHPublicKey(e.getChannel(), Packet.DH_KEY);
		} else {
			super.channelConnected(ctx, e);
		}
	}


	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!shared) {
			Object msg = e.getMessage();
			if (msg instanceof Packet && ((Packet) msg).isProtocol(Packet.DH_KEY)){
				this.receiveDHPublicKey(msg);
				this.sendDHPublicKey(e.getChannel(), Packet.ACK_DH_KEY);
			} else if (msg instanceof Packet && ((Packet) msg).isProtocol(Packet.ACK_DH_KEY)) {
				this.receiveDHPublicKey(msg);
				byte[] key = generateKey();
				cipherHolder.setCipher(AESCipher.getInstance(key));
				shared = true;
				Channels.fireChannelConnected(e.getChannel(), remoteAddress);
			}
		} else {
			super.messageReceived(ctx, e);
		}
	}

	private void sendDHPublicKey(Channel channel, int protocol) {
		byte[] keyBytes = dhKeyPair.getPublic().getEncoded();
		Packet p = new Packet(8);
		p = Packet.fillDHKeyPacket(p, protocol, keyBytes);
		if (logger.isDebugEnabled())
			logger.debug("writing DH control packet " + p);
		channel.write(p);
	}

	private void receiveDHPublicKey(Object msg) throws SkipException {
	    Packet p = (Packet) msg;
	    byte[] keyBytes = p.wrapPacketData().array();
	    KeyFactory kf;
	    try {
	    	kf = KeyFactory.getInstance("DH");
	    	X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(keyBytes);
	    	peerDHPublicKey = kf.generatePublic(x509Spec);
	    } catch (NoSuchAlgorithmException err) {
	    	throw new SkipException("DH algorithm not supported.", err);
	    } catch (InvalidKeySpecException err) {
	    	throw new SkipException("Invalid public key", err);
	    }
    }

	// 生成密钥
	public byte[] generateKey() throws SkipException {
		KeyAgreement ka;
		try {
			ka = KeyAgreement.getInstance("DH");
			ka.init(dhKeyPair.getPrivate());
			ka.doPhase(peerDHPublicKey, true);
			return ka.generateSecret();
		} catch (NoSuchAlgorithmException e) {
			throw new SkipException("DH algorithm not supported.", e);
		} catch (InvalidKeyException e) {
			throw new SkipException("Invalid private key.", e);
		}
	}
}
