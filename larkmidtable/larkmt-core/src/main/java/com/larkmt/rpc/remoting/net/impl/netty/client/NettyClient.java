package com.larkmt.rpc.remoting.net.impl.netty.client;

import com.larkmt.rpc.remoting.net.Client;
import com.larkmt.rpc.remoting.net.params.XxlRpcRequest;
import com.larkmt.rpc.remoting.net.common.ConnectClient;

/**
 * netty client
 *
 * @author xuxueli 2015-11-24 22:25:15
 */
public class NettyClient extends Client {

	private Class<? extends ConnectClient> connectClientImpl = NettyConnectClient.class;

	@Override
	public void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception {
		ConnectClient.asyncSend(xxlRpcRequest, address, connectClientImpl, xxlRpcReferenceBean);
	}

}
