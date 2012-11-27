/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.client; 
 
public interface TunnelConfig {
	/**
	 * 是否启用压缩
	 * 
	 * @return
	 */
	public boolean isCompressed();

	/**
	 * 获取压缩率</br> &nbsp; &nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
	 *          压缩后文件大小 </br>
	 * 压缩率= --------------------------- X 100%</br> &nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 
	 *          原文件大小</br>
	 * 
	 * 当没有启用压缩时，返回1.0
	 * @return
	 */
	public double getCompressionRatio();
	
	/**
	 * 
	 * @param compressed
	 * @param uncompressed
	 */
	public void setCompressRatio(int compressed, int uncompressed);
	
	/**
	 * 获取压缩级别，1-9，如果没有压缩，返回0
	 * @return
	 */
	public int getCompressionLevel();
	
	/**
	 * 设置是否压缩以及压缩级别，当传入的compressed=false时，忽略level参数。
	 * @param compressed
	 * @param level
	 */
	public void setCompressed(boolean compressed,int level);
	/**
	 * 设置是否压缩以及默认压缩级别（level=6）
	 * @param compressed
	 */
	public void setCompressed(boolean compressed);
	
	/**
	 * 是否启用加密
	 * @return
	 */
	public boolean isEncrypted();
	
	/**
	 * set tunnel pipe to be encrypted
	 * @throws Exception when  shared key not generated or cipher not set
	 */
	public void setEncrypted() throws Exception;
}
