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

def common_plain_faraday_client(method, url, token: nil, body: nil, headers: {}, multipart: false)
  Faraday.new(url: api_base_url) do |conn|
    conn.headers["Authorization"] = "Token #{token}" if token
    conn.headers["Accept"] = "application/json"
    conn.headers.update(headers)
    conn.headers["Content-Type"] = "application/json" unless multipart
    conn.request :multipart if multipart
    conn.request :url_encoded
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter

    yield(conn) if block_given?
  end.public_send(method, url) do |req|
    if multipart && body
      req.body = body
    elsif body
      req.body = body.to_json
    end
  end
end

def json_client_get(url, headers: {}, token: nil)
  common_plain_faraday_client(:get, url, token: token, headers: headers)
end

def json_client_post(url, body: nil, token: nil)
  common_plain_faraday_client(:post, url, token: token, body: body)
end

def json_client_delete(url, token: nil)
  common_plain_faraday_client(:delete, url, token: token)
end

def json_client_put(url, body: nil, token: nil)
  common_plain_faraday_client(:put, url, token: token, body: body)
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


# def multipart_faraday_client(form_data, url, method: :post, token: nil)
#   url = URI.parse(api_base_url + url)
#
#   # Prepare the multipart body
#   multipart_body = []
#   form_data.each do |key, value|
#     if value.is_a?(Array)
#       value.each do |v|
#         multipart_body << [key, v.is_a?(File) ? Faraday::Multipart::FilePart.new(v.path, 'application/octet-stream') : v]
#       end
#     else
#       multipart_body << [key, value.is_a?(File) ? Faraday::Multipart::FilePart.new(value.path, 'application/octet-stream') : value]
#     end
#   end
#
#   puts "Multipart body: #{multipart_body.inspect}"
#
#
#   # Make the request using Faraday
#   Faraday.new do |conn|
#     conn.headers["Authorization"] = "Token #{token}" if token
#     conn.headers["Accept"] = "application/json"
#     conn.request :multipart
#     conn.request :url_encoded
#     conn.response :json, content_type: /\bjson$/
#     conn.adapter Faraday.default_adapter
#   end.public_send(method, url) do |req|
#     req.body = multipart_body
#   end
# end

# def multipart_faraday_client(form_data, url, method: :post, token: nil)
#   url = URI.parse(api_base_url + url)
#
#   # Prepare the multipart body
#   multipart_body = []
#   form_data.each do |key, value|
#     if value.is_a?(Array)
#       # Add each file or value as a separate part with the same key
#       value.each do |v|
#         multipart_body << [key, v.is_a?(File) ? Faraday::Multipart::FilePart.new(v.path, 'application/octet-stream') : v]
#       end
#     else
#       # Add single value
#       multipart_body << [key, value.is_a?(File) ? Faraday::Multipart::FilePart.new(value.path, 'application/octet-stream') : value]
#     end
#   end
#
#   # Make the request using Faraday
#   Faraday.new do |conn|
#     conn.headers["Authorization"] = "Token #{token}" if token
#     conn.headers["Accept"] = "application/json"
#     conn.request :multipart
#     conn.request :url_encoded
#     conn.response :json, content_type: /\bjson$/
#     conn.adapter Faraday.default_adapter
#   end.public_send(method, url) do |req|
#     req.body = multipart_body
#   end
# end



def multipart_faraday_client(form_data, url, method: :post, token: nil)
  url = URI.parse(api_base_url + url)

  # Prepare the multipart body
  multipart_body = []
  form_data.each do |key, value|
    if value.is_a?(Array)
      # Add each file or value as a separate part with the same key
      value.each do |v|
        multipart_body << [key, v.is_a?(File) ? Faraday::Multipart::FilePart.new(v.path, 'image/png') : v]
      end
    else
      # Add single value or empty string for optional fields
      multipart_body << [key, value.is_a?(File) ? Faraday::Multipart::FilePart.new(value.path, 'image/png') : (value || "")]
    end
  end

  # Make the request using Faraday
  Faraday.new do |conn|
    conn.headers["Authorization"] = "Token #{token}" if token
    conn.headers["Accept"] = "application/json"
    conn.request :multipart
    conn.request :url_encoded
    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end.public_send(method, url) do |req|
    req.body = multipart_body
  end
end

# works
# def upload_files_with_http(url, form_data, headers: { "Accept" => "application/json" })
#   uri = URI.parse(url)
#   http = Net::HTTP.new(uri.host, uri.port)
#
#   # Prepare the request
#   request = Net::HTTP::Post.new(uri)
#   headers.each { |key, value| request[key] = value }
#
#   # Set form data as multipart
#   request.set_form(form_data, 'multipart/form-data')
#
#   # Send the request
#   response = http.request(request)
#
#   # Parse the response
#   {
#     status: response.code.to_i,
#     body: JSON.parse(response.body)
#   }
# end

def upload_files_with_http(url, form_data, method: :post, headers: { "Accept" => "application/json" })
  uri = URI.parse(url)
  http = Net::HTTP.new(uri.host, uri.port)

  # Prepare the request
  request_class = case method
                  when :post then Net::HTTP::Post
                  when :put then Net::HTTP::Put
                  when :patch then Net::HTTP::Patch
                  else
                    raise ArgumentError, "Unsupported HTTP method: #{method}"
                  end

  request = request_class.new(uri)
  headers.each { |key, value| request[key] = value }

  # Set form data as multipart
  request.set_form(form_data, 'multipart/form-data')

  # Send the request
  response = http.request(request)

  # Parse the response
  {
    status: response.code.to_i,
    body: JSON.parse(response.body)
  }
end




# require 'net/http'
# require 'json'

def upload_files_with_http2(url, form_data, method: :post, headers: { "Accept" => "application/json" })
  uri = URI.parse(api_base_url + url)
  http = Net::HTTP.new(uri.host, uri.port)

  # Prepare the request
  request_class = case method
                  when :post then Net::HTTP::Post
                  when :put then Net::HTTP::Put
                  when :patch then Net::HTTP::Patch
                  else
                    raise ArgumentError, "Unsupported HTTP method: #{method}"
                  end

  request = request_class.new(uri)
  headers.each { |key, value| request[key] = value }

  # Prepare the form data for multipart
  prepared_form_data = form_data.map do |key, value|
    if value.is_a?(Array)
      # Handle arrays, such as multiple files
      value.map { |v| [key.to_s, v] }
    else
      # Single value
      [[key.to_s, value]]
    end
  end.flatten(1)

  # Set form data as multipart
  request.set_form(prepared_form_data, 'multipart/form-data')

  # Send the request
  response = http.request(request)

  # Parse the response
  {
    status: response.code.to_i,
    body: JSON.parse(response.body)
  }
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
