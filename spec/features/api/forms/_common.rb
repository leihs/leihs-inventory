def validate_map_structure(map, required_keys)
  errors = []
  # Check if the number of keys matches
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
    puts "✅ Map structure is valid."
    true
  else
    puts "❌ Validation failed with errors:"
    errors.each { |error| puts "- #{error}" }
    false
  end
end

def expected_form_fields(fields, expected_fields)
  form_field_ids = fields.map { |field| field["id"] }
  puts "Form field IDs: #{form_field_ids}"
  expect(form_field_ids).to eq(expected_fields)
end
