package com.cs.mobile.common.utils.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.cs.mobile.common.exception.api.ExceptionUtils;
import com.cs.mobile.common.utils.JsonUtil;
import com.cs.mobile.common.utils.StringUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP请求工具类
 * 
 * @author songjian
 * @date 2019年1月17日
 */
@Slf4j
public class HttpUtil {
	public static final String DEFAULT_CHARSET = "UTF-8";
	private static final String METHOD_POST = "POST";

	private static final String ERROR_HTML = "<h5 id=\"errorMes\" style=\"color: #ff6600; display: none; font-size: 11px;\">";

	private HttpUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 生成 HTTP查询字符串
	 *
	 * @param params
	 *            请求参数
	 * @param charset
	 *            字符集
	 * @return
	 * @throws Exception
	 */
	public static String buildQuery(Map<String, String> params, String charset) throws Exception {
		if (params == null || params.isEmpty()) {
			return null;
		}

		StringBuilder query = new StringBuilder();
		Set<Map.Entry<String, String>> entries = params.entrySet();
		boolean hasParam = false;
		for (Map.Entry<String, String> entry : entries) {
			String name = entry.getKey();
			String value = entry.getValue();
			// 忽略参数名或参数值为空的参数
			if (StringUtil.areNotEmpty(name, value)) {
				if (hasParam) {
					query.append("&");
				} else {
					hasParam = true;
				}
				if ("__VIEWSTATE".contains(name)) {
					query.append(name).append("=").append(value);
				} else {
					query.append(name).append("=").append(URLEncoder.encode(value, charset));
				}
			}
		}

		return query.toString();
	}

	/**
	 * 执行HTTP POST请求。
	 *
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @return 响应字符串
	 * @throws Exception
	 */
	public static String doPost(String url, Map<String, String> params, int connectTimeout, int readTimeout)
			throws Exception {
		return doPost(url, params, DEFAULT_CHARSET, connectTimeout, readTimeout);
	}

	public static String doPost(String url, Map<String, String> headers, Map<String, String> params, int connectTimeout,
			int readTimeout) throws Exception {
		return doPost(url, headers, params, DEFAULT_CHARSET, connectTimeout, readTimeout);
	}

	/**
	 * 执行HTTP POST请求。
	 *
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param charset
	 *            字符集
	 * @return 响应字符串
	 * @throws Exception
	 */
	public static String doPost(String url, Map<String, String> params, String charset, int connectTimeout,
			int readTimeout) throws Exception {
		String ctype = "application/x-www-form-urlencoded;charset=" + charset;
		String query = buildQuery(params, charset);
		byte[] content = {};
		if (query != null) {
			content = query.getBytes(charset);
		}
		return doPost(url, ctype, null, content, connectTimeout, readTimeout);
	}

	public static String doPost(String url, Map<String, String> headers, Map<String, String> params, String charset,
			int connectTimeout, int readTimeout) throws Exception {
		String ctype = "application/x-www-form-urlencoded;charset=" + charset;
		String query = buildQuery(params, charset);
		byte[] content = {};
		if (query != null) {
			content = query.getBytes(charset);
		}
		return doPost(url, ctype, headers, content, connectTimeout, readTimeout);
	}

	/**
	 * 执行HTTP POST请求。
	 *
	 * @param url
	 *            请求地址
	 * @param ctype
	 *            请求类型
	 * @param content
	 *            请求字节数组
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(String url, String ctype, Map<String, String> headers, byte[] content,
			int connectTimeout, int readTimeout) throws IOException {
		HttpURLConnection conn = null;
		OutputStream out = null;
		String rsp = null;
		try {
			conn = getConnection(new URL(url), METHOD_POST, ctype, headers);

			conn.setConnectTimeout(connectTimeout);
			conn.setReadTimeout(readTimeout);

			out = conn.getOutputStream();
			out.write(content);
			rsp = getResponseAsString(conn);
		} catch (Exception e) {
			ExceptionUtils.wapperBussinessException("调用外部系统网络异常");
		} finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	public static String doPostCookies(String url, Map<String, String> headers, Map<String, String> params,
			String charset, int connectTimeout, int readTimeout) throws Exception {
		String ctype = "application/x-www-form-urlencoded;charset=" + charset;
		String query = buildQuery(params, charset);
		byte[] content = {};
		if (query != null) {
			content = query.getBytes(charset);
		}
		return doPostCookie(url, ctype, headers, content, connectTimeout, readTimeout);
	}

	/**
	 * 执行HTTP POST请求。
	 *
	 * @param url
	 *            请求地址
	 * @param ctype
	 *            请求类型
	 * @param content
	 *            请求字节数组
	 * @return 响应字符串
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public static String doPostCookie(String url, String ctype, Map<String, String> headers, byte[] content,
			int connectTimeout, int readTimeout) throws Exception {
		HttpURLConnection conn = null;
		OutputStream out = null;
		String rsp = null;
		try {
			conn = getConnection(new URL(url), METHOD_POST, ctype, headers);

			conn.setConnectTimeout(connectTimeout);
			conn.setReadTimeout(readTimeout);
			conn.setFollowRedirects(false);
			conn.setInstanceFollowRedirects(false);
			out = conn.getOutputStream();
			out.write(content);
			String body = getResponseAsString(conn);
			System.out.println(body);
			if (body.indexOf(ERROR_HTML) >= 0) {
				String msg = body.substring(body.indexOf(ERROR_HTML));
				msg = msg.substring(ERROR_HTML.length(), msg.indexOf("</h5>"));
				if (StringUtil.notEmpty(msg) && !msg.contains("验证码")) {
					ExceptionUtils.wapperBussinessException(msg);
				}
			}

			if (conn.getHeaderField("Set-Cookie") == null) {
				return rsp;
			}
			Map<String, List<String>> headrs = conn.getHeaderFields();

			List<String> cookies = headrs.get("Set-Cookie");
			rsp = JsonUtil.writeValueAsString(cookies);
		} finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	public static String doPostByBody(String url, Map<String, String> params, Map<String, String> headers,
			int connectTimeout, int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		HttpPost httpPost = new HttpPost(url);
		StringEntity postEntity = new StringEntity(JsonUtil.writeValueAsString(params), "UTF-8");
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("X-Requested-With", "XMLHttpRequest");
		if (headers != null) {
			for (Entry<String, String> entry : headers.entrySet()) {
				httpPost.addHeader(entry.getKey(), entry.getValue());
			}
		}
		httpPost.setEntity(postEntity);
		String result = null;
		// 设置请求器的配置
		httpPost.setConfig(requestConfig);
		try {
			HttpResponse response = httpClient.execute(httpPost);

			HttpEntity entity = response.getEntity();

			result = EntityUtils.toString(entity, "UTF-8");
			System.out.println("entity----" + result);
			Header header = response.getFirstHeader("Set-Cookie");
			;

			result = JsonUtil.writeValueAsString(header.getElements());
			System.out.println("header----" + result);
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpPost.abort();
		}

		return result;
	}

	public static String doPostByBody(String url, List<Map<String, String>> params, int connectTimeout,
			int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		HttpPost httpPost = new HttpPost(url);
		StringEntity postEntity = new StringEntity(JsonUtil.writeValueAsString(params), "UTF-8");
		httpPost.addHeader("Content-Type", "text/json");
		httpPost.setEntity(postEntity);
		String result = null;
		// 设置请求器的配置
		httpPost.setConfig(requestConfig);
		try {
			HttpResponse response = httpClient.execute(httpPost);

			HttpEntity entity = response.getEntity();

			result = EntityUtils.toString(entity, "UTF-8");
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpPost.abort();
		}

		return result;
	}

	public static InputStream doGetStream(String url, Map<String, String> params, int connectTimeout,
			int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		StringBuffer urlStr = new StringBuffer(url);
		if (params != null && params.size() > 0) {
			for (Entry<String, String> entry : params.entrySet()) {
				if (StringUtil.notEmpty(entry.getValue())) {
					if (urlStr.indexOf("?") == -1)
						urlStr.append("?");
					else
						urlStr.append("&");

					urlStr.append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
		}
		HttpGet httpGet = new HttpGet(urlStr.toString());
		httpGet.setConfig(requestConfig);
		InputStream result = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);

			HttpEntity entity = response.getEntity();

			result = entity.getContent();
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpGet.abort();
		}

		return result;
	}

	public static String doGet(String url, Map<String, String> params, int connectTimeout, int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		StringBuffer urlStr = new StringBuffer(url);
		if (params != null && params.size() > 0) {
			for (Entry<String, String> entry : params.entrySet()) {
				if (StringUtil.notEmpty(entry.getValue())) {
					if (urlStr.indexOf("?") == -1)
						urlStr.append("?");
					else
						urlStr.append("&");

					urlStr.append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
		}
		HttpGet httpGet = new HttpGet(urlStr.toString());
		httpGet.setConfig(requestConfig);
		String result = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);

			HttpEntity entity = response.getEntity();

			result = EntityUtils.toString(entity, "UTF-8");
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpGet.abort();
		}

		return result;
	}

	public static String doGet(String url, Map<String, String> headers, Map<String, String> params, int connectTimeout,
			int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		StringBuffer urlStr = new StringBuffer(url);
		if (params != null && params.size() > 0) {
			for (Entry<String, String> entry : params.entrySet()) {
				if (StringUtil.notEmpty(entry.getValue())) {
					if (urlStr.indexOf("?") == -1)
						urlStr.append("?");
					else
						urlStr.append("&");

					urlStr.append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
		}
		HttpGet httpGet = new HttpGet(urlStr.toString());
		if (headers != null && headers.size() > 0) {
			for (Entry<String, String> entry : headers.entrySet()) {
				httpGet.addHeader(entry.getKey(), entry.getValue());
			}
		}
		httpGet.setConfig(requestConfig);
		String result = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);

			HttpEntity entity = response.getEntity();

			result = EntityUtils.toString(entity, "UTF-8");
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpGet.abort();
		}

		return result;
	}

	public static String doGet(String url, String ctype, Map<String, String> params, int connectTimeout,
			int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		StringBuffer urlStr = new StringBuffer(url);
		if (params != null && params.size() > 0) {
			for (Entry<String, String> entry : params.entrySet()) {
				if (StringUtil.notEmpty(entry.getValue())) {
					if (urlStr.indexOf("?") == -1)
						urlStr.append("?");
					else
						urlStr.append("&");

					urlStr.append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
		}
		HttpGet httpGet = new HttpGet(urlStr.toString());
		httpGet.setConfig(requestConfig);
		String result = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);

			HttpEntity entity = response.getEntity();

			if (StringUtil.areNotEmpty(ctype)) {
				result = EntityUtils.toString(entity, ctype);
			} else {
				result = EntityUtils.toString(entity, "UTF-8");
			}
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpGet.abort();
		}

		return result;
	}

	public static String doGetCookie(String url, Map<String, String> params, int connectTimeout, int socketTimeout) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectTimeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().build();
		StringBuffer urlStr = new StringBuffer(url);
		if (params != null && params.size() > 0) {
			for (Entry<String, String> entry : params.entrySet()) {
				if (StringUtil.notEmpty(entry.getValue())) {
					if (urlStr.indexOf("?") == -1)
						urlStr.append("?");
					else
						urlStr.append("&");

					urlStr.append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
		}
		HttpGet httpGet = new HttpGet(urlStr.toString());
		httpGet.setConfig(requestConfig);
		String result = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);

			Header header = response.getFirstHeader("Set-Cookie");

			result = JsonUtil.writeValueAsString(header.getElements());
		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpGet.abort();
		}

		return result;
	}

	private static HttpURLConnection getConnection(URL url, String method, String ctype, Map<String, String> headers)
			throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "text/xml,text/javascript,text/html,application/json");
		conn.setRequestProperty("User-Agent", "yiyun-sdk-java");
		conn.setRequestProperty("Content-Type", ctype);

		if (headers != null && headers.size() > 0) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				// 忽略参数名或参数值为空的参数
				if (StringUtil.areNotEmpty(name, value)) {
					conn.setRequestProperty(name, value);
				}
			}
		}
		return conn;
	}

	protected static String getResponseAsString(HttpURLConnection conn) throws IOException {
		String charset = getResponseCharset(conn.getContentType());
		InputStream es = conn.getErrorStream();
		if (es == null) {
			return getStreamAsString(conn.getInputStream(), charset);
		} else {
			String msg = getStreamAsString(es, charset);
			if (StringUtil.isEmpty(msg)) {
				throw new IOException(conn.getResponseCode() + ":" + conn.getResponseMessage());
			} else {
				throw new IOException(msg);
			}
		}
	}

	private static String getStreamAsString(InputStream stream, String charset) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
			StringWriter writer = new StringWriter();

			char[] chars = new char[256];
			int count = 0;
			while ((count = reader.read(chars)) > 0) {
				writer.write(chars, 0, count);
			}

			return writer.toString();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	private static String getResponseCharset(String ctype) {
		String charset = DEFAULT_CHARSET;

		if (!StringUtil.isEmpty(ctype)) {
			String[] params = ctype.split(";");
			for (String param : params) {
				param = param.trim();
				if (param.startsWith("charset")) {
					String[] pair = param.split("=", 2);
					if (pair.length == 2) {
						if (!StringUtil.isEmpty(pair[1])) {
							charset = pair[1].trim();
						}
					}
					break;
				}
			}
		}

		return charset;
	}

	public static String getRequestFullUriNoContextPath(HttpServletRequest request) {
		String port = "";
		if (request.getServerPort() != 80) {
			port = ":" + request.getServerPort();
		}
		return request.getScheme() + "://" + request.getServerName() + port + request.getContextPath()
				+ request.getServletPath();
	}

	/**
	 * 重定向到http://的url
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @param url
	 */
	public static void redirectHttpUrl(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
			String url) {
		try {
			httpServletResponse.sendRedirect(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getRemoteHost(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (!StringUtil.isEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
			// 多次反向代理后会有多个ip值，第一个ip才是真实ip
			int index = ip.indexOf(",");
			if (index != -1) {
				log.debug(new StringBuffer("请求客户端IP[many-X-Forwarded-For]地址：")
						.append(replaceLocalIp(ip.substring(0, index))).toString());
				return replaceLocalIp(ip.substring(0, index));
			} else {
				log.debug(new StringBuffer("请求客户端IP[one-X-Forwarded-For]地址：").append(replaceLocalIp(ip)).toString());
				return replaceLocalIp(ip);
			}
		}
		ip = request.getHeader("X-Real-IP");
		if (!StringUtil.isEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
			log.debug(new StringBuffer("请求客户端IP[X-Real-IP]地址：").append(replaceLocalIp(ip)).toString());
			return replaceLocalIp(ip);
		}
		log.debug(new StringBuffer("请求客户端IP[request.getRemoteAddr()]地址：").append(request.getRemoteAddr()).toString());
		return replaceLocalIp(request.getRemoteAddr());
	}

	private static String replaceLocalIp(String ip) {
		return ip.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ip;
	}

}
