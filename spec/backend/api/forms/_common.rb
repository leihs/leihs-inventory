def validate_map_structure(map, required_keys)
  errors = []
  unless map.keys.size == required_keys.keys.size
    errors << "Key count mismatch: expected #{required_keys.keys.size}, got #{map.keys.size}"
  end

  required_keys.each do |key, expected_classes|
    unless map.key?(key)
      errors << "Missing key: #{key}"
      next
    end

    value = map[key]
    expected_classes = Array(expected_classes)

    unless expected_classes.any? { |cls| value.is_a?(cls) }
      errors << "Invalid type for key '#{key}': expected #{expected_classes.join(" or ")}, got #{value.class}"
    end
  end

  if errors.empty?
    # puts "✅ Map structure is valid."
    true
  else
    puts "❌ Validation failed with errors:"
    errors.each { |error| puts "- #{error}" }
    false
  end
end

def expected_form_fields(fields, expected_fields)
  form_field_ids = fields.map { |field| field["id"] }
  expect(form_field_ids).to eq(expected_fields)
end

def extract_first_level_of_tree(body)
  body['children'].map do |child|
    {
      "id"   => child["category_id"],
      "name" => child["name"]
    }
  end
end

def compare_values(hash1, hash2, keys)
  keys.all? do |key|
    if !hash1.key?(key) || !hash2.key?(key)
      puts "Key #{key} is missing in one of the hashes"
      false
    else
      # puts "\nComparing key: #{key}, values: #{hash1[key]} == #{hash2[key]}"
      # puts "Comparing key-type: #{key}, values: #{hash1[key].class} == #{hash2[key].class}"
      # if hash1[key].to_s != hash2[key].to_s
      #   puts "❌ Values are not equal for key: #{key}"
      #   puts "❌ Values are not equal for key: #{key}, hash1: #{hash1[key]}, hash2: #{hash2[key]}"
      # else
      #   puts "✅ Values are equal for key: #{key}"
      # end

      hash1[key].to_s == hash2[key].to_s
    end
  end
end
