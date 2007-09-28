/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.safehaus.adbcj.mysql;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbListener;
import org.safehaus.adbcj.DbSessionFuture;
import org.safehaus.adbcj.Result;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.safehaus.adbcj.support.Request;

public class MysqlConnectionManager implements ConnectionManager {

	private final SocketConnector socketConnector;
	
	private final String host;
	private final int port;
	
	private final LoginCredentials credentials;
	
	public MysqlConnectionManager(String host, int port, String username, String password, String schema, ExecutorService executorService, Properties properties) {
		int processorCount = Runtime.getRuntime().availableProcessors();
		socketConnector = new SocketConnector(processorCount, executorService);
		//socketConnector.setWorkerTimeout(5); // TODO: Make configurable
		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();
		
		DemuxingProtocolCodecFactory codecFactory = new DemuxingProtocolCodecFactory();
		codecFactory.register(new MessageDecoderFactory() {
			public MessageDecoder getDecoder() throws Exception {
				return new MysqlMessageDecoder();
			}
		});
		codecFactory.register(new LoginRequestEncoder());
		codecFactory.register(new CommandRequestEncoder());

		filterChain.addLast("codec", new ProtocolCodecFilter(codecFactory)); // TODO Make filter name a constant
		
		socketConnector.setHandler(new MysqlProtocolHandler());
		
		this.host = host;
		this.port = port;
		
		this.credentials = new LoginCredentials(username, password, schema);
	}
	
	public DbSessionFuture<Void> close(boolean immediate) throws DbException {
		// TODO: Close all open connections
		return null;
	}

	public boolean isClosed() {
		// TODO: Implement me
		return false;
	}
	
	public DbFuture<Connection> connect() {
		SocketAddress address = new InetSocketAddress(host, port);
		ConnectFuture connectFuture = socketConnector.connect(address);
		
		final DefaultDbFuture<Connection> dbConnectFuture = new DefaultDbFuture<Connection>() {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				return false;
			}
		};
		
		connectFuture.addListener(new IoFutureListener() {
			public void operationComplete(IoFuture future) {
				final MysqlConnection connection = new MysqlConnection(MysqlConnectionManager.this, future.getSession(), credentials);
				IoSessionUtil.setMysqlConnection(future.getSession(), connection);
				
				connection.enqueueRequest(new Request<Result>() {
					public void execute(DefaultDbFuture<Result> future) {
						dbConnectFuture.setValue(connection);
					}
				}).addListener(new DbListener<Result>() {
					public void onCompletion(DbFuture<Result> future) {
						try {
							future.get();
							dbConnectFuture.setValue(connection);
						} catch (DbException e) {
							dbConnectFuture.setException(e);
						} catch (InterruptedException e) {
							dbConnectFuture.setException(new DbException(e));
						} finally {
							dbConnectFuture.setDone();
						}
					}
				});
				
			}
		});
		
		return dbConnectFuture;
	}
	
	@Override
	public String toString() {
		return String.format("%s: mysql://%s:%d/%s (user: %s)", getClass().getName(), host, port, credentials.getDatabase(), credentials.getUserName());
	}

}