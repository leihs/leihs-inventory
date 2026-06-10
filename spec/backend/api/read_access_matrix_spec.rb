require "spec_helper"
require_relative "_shared"
require_relative "shared/read_access_examples"

RSpec.describe "Inventory read API (access matrix)" do
  endpoints = [
    {
      name: "category-tree",
      path_proc: -> { "/inventory/#{pool_id}/category-tree/" },
      body_assertion: ->(body) { expect(body["name"]).to eq("categories") }
    },
    {
      name: "fields",
      path_proc: -> { "/inventory/#{pool_id}/fields/?target_type=license" },
      body_assertion: ->(body) { expect(body["fields"]).to be_an(Array) }
    },
    {
      name: "list",
      path_proc: -> { "/inventory/#{pool_id}/list/" },
      body_assertion: ->(body) { expect(body).to be_an(Array) }
    },
    {
      name: "inventory-pools",
      path_proc: -> { "/inventory/#{pool_id}/inventory-pools/?responsible=true" },
      body_assertion: ->(body) { expect(body).to be_an(Array) }
    }
  ].freeze

  %w[group_manager inventory_manager].each do |role|
    context "as #{role}" do
      endpoints.each do |endpoint|
        context endpoint[:name] do
          include_examples "read endpoint access",
            role: role,
            path_proc: endpoint[:path_proc],
            body_assertion: endpoint[:body_assertion]
        end
      end
    end
  end
end
