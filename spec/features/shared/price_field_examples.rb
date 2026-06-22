# Shared examples for PriceField behaviour.
# Assumes en-GB locale (decimal ".", thousands ",").
# Include inside a context where the price field is already visible on the page.
#
# Usage:
#   include_examples "price field", field_label: "Price"
#   include_examples "price field", field_label: "Initial Price"

shared_examples "price field" do |field_label:|
  it "formats whole number with two decimal places on blur" do
    fill_in field_label, with: "100"
    find_field(field_label).send_keys(:tab)
    expect(find_field(field_label).value).to eq "100.00"
  end

  it "preserves whole number when fraction is deleted" do
    fill_in field_label, with: "100.00"
    find_field(field_label).send_keys(:tab)
    field = find_field(field_label)
    3.times { field.send_keys(:backspace) }
    field.send_keys(:tab)
    expect(find_field(field_label).value).to eq "100.00"
  end

  it "discards extra fraction digits beyond two" do
    fill_in field_label, with: "1.00500"
    find_field(field_label).send_keys(:tab)
    expect(find_field(field_label).value).to eq "1.00"
  end

  it "ignores trailing non-numeric characters" do
    fill_in field_label, with: "1.00abc"
    find_field(field_label).send_keys(:tab)
    expect(find_field(field_label).value).to eq "1.00"
  end
end
