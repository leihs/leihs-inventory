require "spec_helper"
require_relative "../_shared"

RSpec.describe "Inventory profile API (language_to_use)" do
  include_context :setup_api, "inventory_manager"
  include_context :generate_session_header

  let(:client) { plain_faraday_json_client(@cookie_header) }

  def language_to_use
    resp = client.get "/inventory/profile/"
    expect(resp.status).to eq(200)
    resp.body.dig("language_to_use", "locale")
  end

  def ensure_language(locale, active:)
    if database[:languages].where(locale: locale).empty?
      database[:languages].insert(locale: locale, name: locale, default: false, active: active)
    else
      database[:languages].where(locale: locale).update(active: active)
    end
  end

  let(:default_locale) { database[:languages].where(default: true).first[:locale] }

  it "returns the language of the user if it is active" do
    ensure_language("fr-CH", active: true)
    database[:users].where(id: @user.id).update(language_locale: "fr-CH")
    expect(language_to_use).to eq("fr-CH")
  end

  it "returns the default language if the language of the user is inactive" do
    ensure_language("fr-CH", active: false)
    database[:users].where(id: @user.id).update(language_locale: "fr-CH")
    expect(language_to_use).to eq(default_locale)
  end

  it "returns the default language if the user has no language" do
    database[:users].where(id: @user.id).update(language_locale: nil)
    expect(language_to_use).to eq(default_locale)
  end
end
