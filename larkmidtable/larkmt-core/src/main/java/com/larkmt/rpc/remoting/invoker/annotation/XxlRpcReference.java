package com.larkmt.rpc.remoting.invoker.annotation;

import com.larkmt.rpc.remoting.invoker.call.CallType;
import com.larkmt.rpc.remoting.invoker.route.LoadBalance;
import com.larkmt.rpc.remoting.net.Client;
import com.larkmt.rpc.remoting.net.impl.netty.client.NettyClient;
import com.larkmt.rpc.serialize.Serializer;
import com.larkmt.rpc.serialize.impl.HessianSerializer;

import java.lang.annotation.*;

/**
 *
 * @Author: LarkMidTable
 * @Date: 2020/9/16 11:14
 * @Description: rpc service annotation, skeleton of stub ("@Inherited" allow service use "Transactional")
 **/
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlRpcReference {

	Class<? extends Client> client() default NettyClient.class;

	Class<? extends Serializer> serializer() default HessianSerializer.class;

	CallType callType() default CallType.SYNC;

	LoadBalance loadBalance() default LoadBalance.ROUND;

	//Class<?> iface;
	String version() default "";

	long timeout() default 1000;

	String address() default "";

	String accessToken() default "";

	//XxlRpcInvokeCallback invokeCallback() ;

}
