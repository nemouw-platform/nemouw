# Specialist Discovery

How visitors find specialists on the site. Everything here is **public**: no login, no cookies needed.

Base URL: `https://api.nemouw.com/v1` (local: `http://localhost:8080/v1`)

## The 3 endpoints

| Endpoint                         | What it does                                                              |
|----------------------------------|---------------------------------------------------------------------------|
| `GET /specialists`               | Search and list specialists (the results page)                            |
| `GET /specialists/search/fields` | Lists the fields you can filter and sort by (build the filter UI from it) |
| `GET /specialists/{username}`    | One specialist's full public profile                                      |

There is also `POST /specialists/{username}/events` to record a profile view or WhatsApp click (see the end).

## Who shows up

Only specialists whose account is **active and verified**. Suspended, rejected, pending or deleted specialists never appear in search, and their profile returns `404`.

## How a search works

1. You send query parameters. All of them are optional, and all of them are combined with **AND**.
2. The API keeps only the specialists that match every parameter.
3. For each specialist it picks **one location** to show (see "Which location is shown").
4. It sorts, then returns one page.

No parameters at all is valid: you get everyone, best matches first.

## Search parameters

| Parameter           | Example                           | What it does                                                                                                                                                                          |
|---------------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `search`            | `search=child anxiety`            | Free text. **Every word** must match the name, headline, bio, specialty, condition or place. Small typos are tolerated on long words. Accents and Arabic letter variants are ignored. |
| `location`          | `location=beirut`                 | City or address words. Every word must match the **same** location.                                                                                                                   |
| `country_id`        | `country_id=<uuid>`               | Only specialists with a location in that country.                                                                                                                                     |
| `lat`, `lng`        | `lat=33.89&lng=35.50`             | "Near me". Send both or neither.                                                                                                                                                      |
| `radius_km`         | `radius_km=10`                    | Radius around `lat`/`lng`. Default `25`, min `0.1`, max `200`.                                                                                                                        |
| `condition_ids`     | `condition_ids=<uuid>,<uuid>`     | Treats **any of** these conditions (max 10). More matches rank higher.                                                                                                                |
| `patient_age`       | `patient_age=8`                   | Specialists whose age range includes this age (0 to 120). Specialists with no age range set are kept. An open end counts as unlimited: only `min_age_treated=12` means 12 and up.     |
| `available_days`    | `available_days=monday,wednesday` | Works on **any of** these weekdays (`monday` to `sunday`).                                                                                                                            |
| `available_from`    | `available_from=09:00`            | Wanted start time, 24h `HH:mm`. Keeps weekly windows that end after it.                                                                                                               |
| `available_to`      | `available_to=13:00`              | Wanted end time, 24h `HH:mm`. Keeps weekly windows that start before it. Must be after `available_from`.                                                                              |
| `filter[...]`       | `filter[gender]=female`           | Filter by any field, see below.                                                                                                                                                       |
| `sort`              | `sort=-years_of_experience`       | Order of results, see below.                                                                                                                                                          |
| `locale`            | `locale=fr`                       | Language of `title` and `headline`: `en`, `ar`, `fr`. Default `en`. Falls back to English, then any language the specialist wrote.                                                    |
| `page`, `page_size` | `page=2&page_size=20`             | Paging. Default page `1`, page size `20`, max `50`.                                                                                                                                   |

`search` ignores filler words like "dr", "doctor", "the", "de", "pour", "دكتور". Words shorter than 2 characters are ignored. At most 6 words are used.

### What `search` looks at

Each word is checked against these fields. The higher the weight, the more it pushes a specialist up in `recommended` order.

| Field      | Weight | Typos tolerated | Includes                                                   |
|------------|--------|-----------------|------------------------------------------------------------|
| Name       | 50     | yes             | first name, last name, username                            |
| Specialty  | 40     | yes             | name and its English, French and Arabic labels             |
| Conditions | 40     | yes             | every treated condition, with all language labels          |
| Headline   | 30     | no              | title and headline, in every language the specialist wrote |
| Place      | 20     | no              | city, label and address lines of every location            |
| Bio        | 10     | no              | bio, in every language                                     |

How it matches:

- **Every word must hit at least one field.** `search=anxiety beirut` needs "anxiety" somewhere and "Beirut" somewhere, not necessarily in the same field.
- A word matches inside longer words: `cardio` finds "cardiologist".
- Upper/lower case, accents and Arabic letter variants are ignored.
- Typo tolerance (for example `cardiolgist`) only applies to words of 4+ letters, and only on name, specialty and conditions. It scores half of an exact hit.
- `locale` does not limit the search: text in all languages is searched.
- Not searched: country, gender, language, prices, credentials, syndicate. Use filters for those.

For place only, use `location`: it matches the same place text but forces all words onto a single location.

### Availability: days and hours

Specialists set **weekly** hours, for example "Monday 09:00 to 13:00 at the Beirut clinic". There are no calendar dates and no booking yet.

- Send `available_days`, or `available_from` / `available_to`, or all of them.
- When you send days and hours together, **one single window** must fit both. `available_days=monday&available_from=18:00` means "works Monday evening".
- A window fits the hours if it overlaps them. Asking for `09:00` to `12:00` matches a window of `10:00` to `14:00`.
- Overnight ranges are not supported: `available_to` must be after `available_from`.

### Place and availability filters work together

`location`, `country_id`, `lat`/`lng` and the availability filters all apply to the **same location**. So `location=beirut&available_days=monday` means "has a Monday window **at a Beirut location**", not "is in Beirut and works Mondays somewhere".

## Filters: `filter[field]`

Format:

- `filter[field]=a,b` means equals **any of** a or b (max 10 values)
- `filter[field][op]=value` uses an operator

Operators: `eq` (default), `gt`, `gte`, `lt`, `lte`, `contains`, `is_null`.

### Fields

| Field                 | Type   | Allowed operators             | Notes                     |
|-----------------------|--------|-------------------------------|---------------------------|
| `specialty_id`        | uuid   | eq, is_null                   |                           |
| `syndicate_id`        | uuid   | eq, is_null                   |                           |
| `username`            | text   | eq, contains, is_null         |                           |
| `first_name`          | text   | eq, contains, is_null         |                           |
| `last_name`           | text   | eq, contains, is_null         |                           |
| `title`               | text   | eq, contains, is_null         | In the requested `locale` |
| `headline`            | text   | eq, contains, is_null         | In the requested `locale` |
| `gender`              | enum   | eq, is_null                   | `male`, `female`          |
| `primary_language`    | enum   | eq, is_null                   | `en`, `ar`, `fr`          |
| `years_of_experience` | number | eq, gt, gte, lt, lte, is_null |                           |
| `min_age_treated`     | number | eq, gt, gte, lt, lte, is_null |                           |
| `max_age_treated`     | number | eq, gt, gte, lt, lte, is_null |                           |
| `session_price_min`   | number | eq, gt, gte, lt, lte, is_null |                           |
| `session_price_max`   | number | eq, gt, gte, lt, lte, is_null |                           |
| `price_currency`      | enum   | eq, is_null                   | `USD`                     |
| `created_at`          | date   | eq, gt, gte, lt, lte, is_null | `YYYY-MM-DD`              |

Rules:

- Text `eq` ignores upper/lower case. Text `contains` also ignores accents.
- `is_null` takes `true` or `false`, e.g. `filter[session_price_max][is_null]=true`.
- Enum values are exact lowercase codes. `Female` is rejected.
- Phone numbers and review data cannot be filtered.

Don't hardcode this table in the UI. Call `GET /specialists/search/fields` and build the filters from it. New fields show up there automatically.

## Sorting: `sort`

Comma separated, `-` in front means descending, max 3 keys. Example: `sort=availability,-years_of_experience`.

| Key              | Meaning                                                                    |
|------------------|----------------------------------------------------------------------------|
| `recommended`    | **Default.** Best match first (see below).                                 |
| `distance`       | Nearest first. Needs `lat` and `lng`.                                      |
| any filter field | e.g. `years_of_experience`, `session_price_min`, `last_name`, `created_at` |

Rules:

- `recommended` and `distance` cannot have a `-` in front.
- Empty values always go last.
- If two specialists tie, the order stays stable, so pages never repeat or skip someone.

### What "recommended" means

1. Text and condition match (if you searched)
2. Profile completeness: photo, bio, treated conditions, availability, and a verified credential
3. Years of experience

## Which location is shown

Each result shows one location (city, country_id and distance_km describe it). Among the locations that match all your place and availability filters, the API picks the nearest one (if lat/lng was sent), otherwise the primary location. The other locations are listed in locations.

## Response of `GET /specialists`

```json
{
  "data": [
    {
      "id": "uuid",
      "username": "dr-jane-doe",
      "first_name": "Jane",
      "last_name": "Doe",
      "title": "Clinical Psychologist",
      "headline": "Helping children and teens",
      "specialty_id": "uuid",
      "profile_picture_url": "https://...",
      "years_of_experience": 8,
      "city": "Beirut",
      "country_id": "uuid",
      "distance_km": 2.4,
      "primary_language": "en",
      "session_price_min": 40.00,
      "session_price_max": 60.00,
      "price_currency": "USD",
      "min_age_treated": 4,
      "max_age_treated": 17,
      "treated_condition_ids": ["uuid", "uuid"],
      "locations": [
        { "country_id": "uuid", "city": "Beirut", "is_primary": true },
        { "country_id": "uuid", "city": "Tripoli", "is_primary": false }
      ]
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 134
}
```

## Response of `GET /specialists/{username}`

Same idea, but the full profile:

- Everything in the search result, plus `bio`, `gender`, `cover_image_url`, `whatsapp_number`.
- `locations`: all locations (`label`, `city`, address lines, `latitude`, `longitude`, `phone`, `is_primary`).
- `availabilities`: weekly windows with `location_id`, `day_of_week` (`monday` to `sunday`) and `start_time`/`end_time` as `HH:mm`.
- `credentials`: only **verified** and **not expired** ones, each with the syndicate that verified it.
- `locale`: the language actually used. It can differ from the one you asked for if the specialist did not write that language.
- `404` if the username does not exist or the specialist is not discoverable.
- `whatsapp_number` can be `null`: **hide the contact button** in that case.

## Errors

| Status | When                   | Body                                                                                                                               |
|--------|------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `422`  | A parameter is invalid | `{ "status": 422, "code": "VALIDATION_ERROR", "message": "...", "errors": [{ "field": "...", "code": "...", "message": "..." }] }` |
| `404`  | Profile not found      | Standard error body                                                                                                                |
| `429`  | Too many requests      | Standard error body and a `Retry-After` header (seconds)                                                                           |

A `422` lists **every** bad parameter at once, so you can show all problems together.

Common `422` causes: `lat` without `lng`, an unknown weekday, a time that is not `HH:mm`, `available_to` before `available_from`, `sort=distance` without coordinates, `-` in front of a computed sort, unknown filter field, page size over 50, more than 3 sort keys.

## Examples

> **Testing in Postman:** requests that contain special characters (`[`, `]`, `,`, `:`) must be URL-encoded, otherwise they are rejected. Those examples include an **Encoded** version you can paste directly into Postman. Examples without an Encoded block have no special characters, so they work as written.

Everyone, default order:

```
GET /specialists
```

Free text with a place:

```
GET /specialists?search=anxiety&location=beirut
```

Female specialists who speak Arabic or French, with 5+ years of experience, cheapest first:

```
GET /specialists?filter[gender]=female&filter[primary_language]=ar,fr&filter[years_of_experience][gte]=5&sort=session_price_min
```

Encoded:

```
GET /specialists?filter%5Bgender%5D=female&filter%5Bprimary_language%5D=ar%2Cfr&filter%5Byears_of_experience%5D%5Bgte%5D=5&sort=session_price_min
```

Treats a 7-year-old, works Monday or Wednesday evenings:

```
GET /specialists?patient_age=7&available_days=monday,wednesday&available_from=17:00
```

Encoded:

```
GET /specialists?patient_age=7&available_days=monday%2Cwednesday&available_from=17%3A00
```

Beirut, works weekday mornings (one window must fit):

```
GET /specialists?location=beirut&available_days=monday&available_from=08:00&available_to=12:00
```

Encoded:

```
GET /specialists?location=beirut&available_days=monday&available_from=08%3A00&available_to=12%3A00
```

Within 10 km of me, nearest first:

```
GET /specialists?lat=33.8938&lng=35.5018&radius_km=10&sort=distance
```

Page 2, French content:

```
GET /specialists?search=pediatre&locale=fr&page=2&page_size=10
```

Single profile:

```
GET /specialists/dr-jane-doe?locale=ar
```

## Tracking events

`POST /specialists/{username}/events`

```json
{ "event_type": "profile_view", "visitor_id": "any-id-you-generate" }
```

`event_type` is `profile_view` or `whatsapp_click`. Send a stable `visitor_id` per browser so unique visitors are counted. Duplicates on the same day still return `202`. These counts feed the specialist's own dashboard.