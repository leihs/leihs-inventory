require 'cgi'

module Helpers
  module CookieParserHelper
    extend self

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

  end
end
