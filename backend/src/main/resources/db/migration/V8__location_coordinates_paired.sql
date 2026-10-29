ALTER TABLE specialist_locations
    ADD CONSTRAINT ck_specialist_locations_coordinates_paired
        CHECK ((latitude IS NULL) = (longitude IS NULL));