# require "json_roa/client"
require "faraday"
require "faraday_middleware"

def api_port
  @api_port ||= ENV["API_V2_HTTP_PORT"].presence || 3260
end

def api_base_url
  @api_base_url ||= "http://localhost:#{api_port}/inventory"
end

# def json_roa_client(&)
#   JSON_ROA::Client.connect(
#     api_base_url, raise_error: false, &
#   )
# end

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

def wtoken_header_plain_faraday_json_client_get(token, url, headers: {})
  token_header_plain_faraday_json_client(:get, url, token, headers: headers)
end

def wtoken_header_plain_faraday_json_client_post(token, url, body: nil)
  token_header_plain_faraday_json_client(:post, url, token, body: body)
end

def wtoken_header_plain_faraday_json_client_delete(token, url)
  token_header_plain_faraday_json_client(:delete, url, token)
end

def wtoken_header_plain_faraday_json_client_put(token, url)
  token_header_plain_faraday_json_client(:put, url, token)
end

def token_header_plain_faraday_json_client(method, url, token, body: nil, headers: {})
  Faraday.new(url: api_base_url) do |conn|
    # conn.headers["Authorization"] = "token #{token}"
    conn.headers["Authorization"] = "Token #{token}"
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




# Function to parse leihs-session cookie string
def parse_leihs_session(session_string)
  session_hash = {}

  # Split the session string by & to get individual key-value pairs
  session_parts = session_string.split('&')

  session_parts.each do |session_part|
    key, value = session_part.split('=', 2)
    session_hash[key] = CGI.unescape(value.to_s.strip) if key
  end

  session_hash
end

# Function to parse the cookie string and return a hash of values
def parse_cookie(cookie_string)
  cookie_hash = {}

  # Split by comma to get individual cookie parts
  cookie_parts = cookie_string.split(',')

  cookie_parts.each do |cookie_part|
    # Split each part by semicolon to separate attributes and main value
    key_value_part = cookie_part.split(';').first.strip

    # Further split by = to get key and value
    key, value = key_value_part.split('=', 2)
    next unless key && value # Skip if key or value is nil

    # If the cookie is 'leihs-session', parse the key-value pairs inside it
    if key == 'leihs-session'
      cookie_hash[key] = parse_leihs_session(value)
    else
      cookie_hash[key] = value.strip
    end
  end

  cookie_hash
end
