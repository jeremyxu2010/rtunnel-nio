/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-9-17  Created
*/ 
package com.cloudbility.rtunnel.common; 
 
public interface Compression{
	  static public final int INFLATER=0;
	  static public final int DEFLATER=1;
	  void init(int type, int level);
	  /**
	   * 
	   * @param buf 待压缩的数据buffer
	   * @param start 压缩的的起始位置
	   * @param len  len[0]是压缩数据的长度（即buffer从start到start+len[0]的数据是需要压缩的）
	   * @return 已经压缩的数据buffer，len[0]会更新为压缩后的数据的长度。（即buffer从start到start+len[0]的数据是已经压缩的）
	   */
	  byte[] compress(byte[] buf, int start, int[] len);
	  /**
	   * 
	   * @param buf 待解压的数据buffer
	   * @param start 解压的起始位置
	   * @param len len[0]是需要解压的数据的长度（即buffer从start到start+len[0]的数据是需要解压的）
	   * @return 已经解压的数据buffer，len[0]会更新为解压后的数据的长度。（即buffer从start到start+len[0]的数据是已经解压的）
	   */
	  byte[] uncompress(byte[] buf, int start, int[] len);
	}
