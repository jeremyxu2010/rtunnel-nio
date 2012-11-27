/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.common; 

import com.cloudbility.common.crypto.AESCipher;
 
public class CipherHolder {
	private AESCipher cipher;

	public AESCipher getCipher() {
    	return cipher;
    }

	public void setCipher(AESCipher cipher) {
    	this.cipher = cipher;
    }
}
