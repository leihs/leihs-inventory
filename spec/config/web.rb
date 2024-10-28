require "faraday"
require "faraday_middleware"

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

def plain_faraday_image_client
  @plain_faraday_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "image/jpeg"}
  ) do |conn|
    yield(conn) if block_given?
    conn.adapter Faraday.default_adapter
  end
end

def plain_faraday_json_client
  @plain_faraday_json_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json"}
  ) do |conn|
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end

def plain_faraday_json_client2
  Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json"}
  ) do |conn|
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end

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

def json_client_get(url, headers: {}, token: nil)
  common_plain_faraday_json_client(:get, url, token: token, headers: headers)
end

def json_client_post(url, body: nil, token: nil)
  common_plain_faraday_json_client(:post, url, token: token, body: body)
end

def json_client_delete(url, token: nil)
  common_plain_faraday_json_client(:delete, url, token: token)
end

def json_client_put(url, body: nil, token: nil)
  common_plain_faraday_json_client(:put, url, token: token, body: body)
end

def common_plain_faraday_json_client(method, url, token: nil, body: nil, headers: {})
  Faraday.new(url: api_base_url) do |conn|
    conn.headers["Authorization"] = "Token #{token}" if token
    conn.headers["Content-Type"] = "application/json"
    conn.headers["Accept"] = "application/json"
    conn.headers.update(headers)
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter

    yield(conn) if block_given?
  end.public_send(method, url) do |req|
    req.body = body.to_json if body
  end
end

def session_auth_plain_faraday_json_client(cookie_string)
  @plain_faraday_json_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json", Cookie: cookie_string}
  ) do |conn|
    yield(conn) if block_given?
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
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
