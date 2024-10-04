require "faraday"
require "faraday_middleware"

def plain_faraday_client
  Faraday.new(
    url: http_base_url,
    headers: {accept: "application/json"}
  ) do |conn|
    conn.adapter Faraday.default_adapter
    conn.response :json, content_type: /\bjson$/
  end
end


def plain_faraday_json_client

  logger = Selenium::WebDriver.logger

  # logger = Logger.new('faraday_debug.log')
  # Logger logger = Logger.getLogger("");

  Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json"}
  ) do |conn|
    # Add request logging
    conn.request :url_encoded

    # Custom logger for file logging
    conn.response :logger, logger, { headers: true, bodies: true }

    yield(conn) if block_given?

    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
end


def plain_faraday_json_client2

  logger = Selenium::WebDriver.logger

  # logger = Logger.new('faraday_debug.log')
  # Logger logger = Logger.getLogger("");

  @plain_faraday_json_client ||= Faraday.new(
    url: api_base_url,
    headers: {accept: "application/json"}
  ) do |conn|
    # Add request logging
    conn.request :url_encoded

    # Custom logger for file logging
    conn.response :logger, logger, { headers: true, bodies: true }

    yield(conn) if block_given?

    conn.response :json, content_type: /\bjson$/
    conn.adapter Faraday.default_adapter
  end
  @plain_faraday_json_client
end