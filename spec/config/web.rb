require "faraday"
require "faraday_middleware"

ACCEPT_PNG = "image/png"
ACCEPT_CSV = "text/csv"
ACCEPT_HTML = "text/html"
ACCEPT_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
X_CSRF_TOKEN = "test-csrf-123-456"

def login_and_extract_session_token(user)
  resp = basic_auth_plain_faraday_json_client(user.login, user.password).get("/sign-in")
  expect(resp.status).to eq(200)

  parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
end

def http_port
  @port ||= Integer(ENV["LEIHS_INVENTORY_HTTP_PORT"].presence || 3260)
end

def http_host
  @host ||= ENV["LEIHS_INVENTORY_HTTP_HOST"].presence || "localhost"
end

def api_base_url
  @http_base_url ||= "http://#{http_host}:#{http_port}"
end

def plain_faraday_client
  @plain_faraday_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "*/*"}
  ) do |conn|
    yield(conn) if block_given?
    conn.adapter Faraday.default_adapter
  end
end

def plain_faraday_resource_client(headers = {})
  @plain_faraday_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "image/jpeg"}.merge(headers)
  ) do |conn|
    yield(conn) if block_given?
    conn.adapter Faraday.default_adapter
  end
end

def plain_faraday_json_client(headers = nil)
  cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

  @plain_faraday_json_client ||= Faraday.new(
    url: api_base_url,
    headers: headers || {:accept => "application/json", :Cookie => cookie.to_s, "x-csrf-token" => X_CSRF_TOKEN}
  ) do |conn|
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end

@deprecated
def basic_auth_plain_faraday_json_client(login, password)
  @basic_auth_plain_faraday_json_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json"}
  ) do |conn|
    conn.request :basic_auth, login, password
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end

def wtoken_header_plain_faraday_json_client(token)
  @plain_faraday_json_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json", Authorization: "token #{token}"}
  ) do |conn|
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end

def common_plain_faraday_client(method, url, token: nil, body: nil, headers: {}, multipart: false, is_binary: false)
  Faraday.new(url: api_base_url) do |conn|
    conn.headers["Authorization"] = "Token #{token}" if token
    conn.headers["Accept"] = "application/json"
    conn.headers["x-csrf-token"] = X_CSRF_TOKEN
    conn.headers["Content-Type"] = "application/json" unless multipart
    # conn.headers["Content-Type"] = "application/x-www-form-urlencoded" if multipart
    conn.headers.update(headers)
    conn.request :multipart if multipart
    conn.request :url_encoded
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter

    yield(conn) if block_given?
  end.public_send(method, url) do |req|
    if (multipart && body) || (is_binary && body)
      req.body = body
    elsif body
      req.body = body.to_json
    end
  end
end

def common_plain_faraday_login_client(method, url, token: nil, body: nil, headers: {})
  Faraday.new(url: api_base_url) do |conn|
    conn.headers["Authorization"] = "Token #{token}" if token
    conn.headers["Accept"] = "text/html,application/xhtml+xml"
    conn.headers["Content-Type"] = "application/x-www-form-urlencoded"
    conn.headers.update(headers)
    conn.request :url_encoded
    conn.adapter Faraday.default_adapter

    yield(conn) if block_given?
  end.public_send(method, url) do |req|
    req.body = body
  end
end

def json_client_get(url, headers: {}, token: nil)
  common_plain_faraday_client(:get, url, token: token, headers: headers)
end

def json_client_post(url, body: nil, headers: {}, token: nil, is_binary: false)
  common_plain_faraday_client(:post, url, token: token, body: body, headers: headers, is_binary: is_binary)
end

def json_client_delete(url, headers: {}, token: nil)
  common_plain_faraday_client(:delete, url, token: token, headers: headers)
end

def json_client_put(url, body: nil, headers: {}, token: nil)
  common_plain_faraday_client(:put, url, token: token, body: body, headers: headers)
end

def json_client_patch(url, body: nil, headers: {}, token: nil)
  common_plain_faraday_client(:patch, url, token: token, body: body, headers: headers)
end

def session_auth_plain_faraday_json_client(cookies: nil, headers: nil)
  headers ||= {"accept" => "application/json"}
  headers[:Cookie] = cookies.map(&:to_s).join("; ") if cookies

  Faraday.new(url: api_base_url, headers: headers) do |conn|
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end

def session_auth_plain_faraday_json_csrf_client(cookies: nil, headers: {"accept" => "application/json", "x-csrf-token" => X_CSRF_TOKEN})
  session_auth_plain_faraday_json_client(cookies: cookies, headers: headers)
end

ResponseResult = Struct.new(:status, :body)

def http_multipart_client(url, form_data, method: :post, headers: {"Accept" => "application/json"}, token: nil)
  uri = URI.parse(api_base_url + url)
  http = Net::HTTP.new(uri.host, uri.port)

  request_class = case method
  when :post then Net::HTTP::Post
  when :put then Net::HTTP::Put
  when :patch then Net::HTTP::Patch
  else
    raise ArgumentError, "Unsupported HTTP method: #{method}"
  end

  request = request_class.new(uri)
  headers["Authorization"] = "Token #{token}" if token
  headers["x-csrf-token"] = X_CSRF_TOKEN
  headers.each { |key, value| request[key] = value }
  prepared_form_data = form_data.flat_map do |key, value|
    if value.is_a?(Array)
      value.map { |v| [key.to_s, v] }
    else
      [[key.to_s, value]]
    end
  end

  request.set_form(prepared_form_data, "multipart/form-data")
  response = http.request(request)

  ResponseResult.new(response.code.to_i, JSON.parse(response.body))
end

#### parse cookie fnc ####################################################

def parse_leihs_session(session_string)
  session_hash = {}
  session_parts = session_string.split("&")
  session_parts.each do |session_part|
    key, value = session_part.split("=", 2)
    session_hash[key] = CGI.unescape(value.to_s.strip) if key
  end
  session_hash
end

def parse_cookie(cookie_string)
  cookie_hash = {}
  cookie_parts = cookie_string.split(",")
  cookie_parts.each do |cookie_part|
    key_value_part = cookie_part.split(";").first.strip

    key, value = key_value_part.split("=", 2)
    next unless key && value

    cookie_hash[key] = if key == "leihs-session"
      parse_leihs_session(value)
    else
      value.strip
    end
  end

  cookie_hash
end
