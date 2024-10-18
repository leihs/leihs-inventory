require_relative "../../database/spec/config/database"
require "factory_bot"
require "faker"

Sequel::Model.db = database
Sequel::Model.send :alias_method, :save!, :save

FactoryBot.define do
  to_create { |instance| instance.save }
end

RSpec.configure do |config|
  config.include FactoryBot::Syntax::Methods

  config.before(:suite) do
    FactoryBot.definition_file_paths = %w[./database/lib/leihs/constants ./shared-clj/factories ./spec/factories]
    # FactoryBot.definition_file_paths = %w[./database/lib/leihs/constants ./database/spec/factories ./shared-clj/factories ./spec/factories]
    FactoryBot.find_definitions
  end

  config.before(:each) do
    Faker::UniqueGenerator.clear
  end
end


# unless FactoryBot.factories.registered?(:leihs_model)
#   factory :leihs_model do
#     product { Faker::Commerce.product_name }
#
#     transient do
#       categories { [] }
#       items { [] }
#       images { [] }
#       properties { [] }
#       recommends { [] }
#     end
#   end
# end


# require 'factory_bot'
# require 'faker'
#
# Sequel::Model.db = database
#
# FactoryBot.define do
#   to_create { |instance| instance.save }
# end
#
# RSpec.configure do |config|
#   config.include FactoryBot::Syntax::Methods
#
#   config.before(:suite) do
#     FactoryBot.find_definitions
#   end
# end