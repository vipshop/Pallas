/**
 * Copyright 2019 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.pallas.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcUtil {
	private static final Logger logger = LoggerFactory
			.getLogger(JdbcUtil.class);

	private JdbcUtil() {
	}

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ExceptionInInitializerError();
		}
	}

	public static boolean testDBConnect(String ip, String port, String db, String user, String password) {
		Connection conn = null;
		try {
			conn = getConnection(ip, port, db, user, password);
			return true;
		} catch (SQLException e) {
			logger.error(e.getClass() + " " + e.getMessage(), e);
			return false;
		} finally {
			JdbcUtil.free(conn, null, null);
		}
	}
	public static Connection getConnection(String ip, String port, String db, String user,
			String password) throws SQLException {
		return getConnection("jdbc:mysql://" + ip
				+ ":" + port + "/" + db
				+ "?useUnicode=true&characterEncoding=utf-8", user, password);
	}

	public static Connection getConnection(String url, String user,
			String password) throws SQLException {
		try {
			return DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			logger.error("connect db error by url: " + url + " and user: " + user, e);
			throw e;
		}
	}

	public static int executeUpdate(Connection connection, String sql) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement(sql);
			return pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.toString(), e);
			return 0;
		} finally {
			free(connection, pstmt, null);
		}
	}

	public static void free(Connection conn, Statement stmt, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			logger.error(e.getClass() + " " + e.getMessage(), e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				logger.error(e.getClass() + " " + e.getMessage(), e);
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						logger.error(e.getClass() + " " + e.getMessage(), e);
					}
				}
			}
		}

	}
}