

```http
REQUEST_PATH = /search?q=solo+leveling
```

## HTTP Paths

* *genres* - `${REQUEST_PATH}&genres=survival%2Cmonsters&excludeGenres=apocalypse`
* *genreMode* - `${REQUEST_PATH}&genreMode=or` with values of `or` or `and`
* *filterType* - `${REQUEST_PATH}&type=manhwa` with values of `manhwa`, `manhua`, or `manga`
* *filterChapters* - `${REQUEST_PATH}&minChapters=51&maxChapters=100`
* *filterRating* - `${REQUEST_PATH}&minRating=4.5&maxRating=5`
* *filterAuthor* - `${REQUEST_PATH}&author=John+Doe`
* *filterStatus* - `${REQUEST_PATH}&status=ongoing` with values of `ongoing`, `completed`, or `hiatus`
* *filterSort* - `${REQUEST_PATH}&sortBy=rating` with values of `popular`, `trending`, `updated`, `rating`, `liibrary`, `newest`, `chapters`, or `alphabetical`

## Genres

### List of Genres

* Action
* Adaptation
* Adult
* Adventure
* Apocalypse
* Borderline H
* Boys Love
* Cartoon
* Comedy
* Comic
* Cooking
* Crazy MC
* Crime
* Cultivation
* Demons
* Doujinshi
* Drama
* Ecchi
* Explicit Sex
* Fantasy
* Fight
* Full Color
* Game
* Gender Bender
* Genius MC
* Ghosts
* Girls Love
* GL
* Gourmet
* Harem
* Historical
* Horror
* Incest
* Isekai
* Iyashikei
* Josei
* Long strip
* Magic
* Manga
* Manhua
* Manhwa
* Manhwa Hot
* Martial Arts
* Mature
* Mecha
* Military
* Monsters
* Murim
* Mystery
* Noble
* Office workers
* One shot
* Overpowered
* Psychological
* Rebirth
* Regression
* Reincarnation
* Returner
* Revenge
* Reverse
* Romance
* Royal family
* Royalty
* School
* School Life
* Sci-Fi
* Seinen
* Shotacon
* Shoujo
* Shoujo Ai
* Shounen
* Shounen Ai
* Slice of Life
* Smut
* Sport
* Sports
* Strategy
* Supernatural
* System
* Thriller
* Time Travel
* Tower
* Tragedy
* Uncensored
* Vampire
* Villain
* Virtual Reality
* Webtoon
* Webtoons
* Wuxia

### Genre Operators
* OR - `genreMode=or`
* AND - `genreMode=and`

### Genre Presets

Presets are rounded off to individual genres, and can be used to filter out unwanted genres or to include only the genres you want. They 
are not defined by the API, but are a convenience for users to quickly filter out genres they don't want to see.

#### 💔 No Romance

###### _Excludes_
* Harem
* Josei
* Reverse
* Romance
* Shoujo
* Shoujo Ai

#### ⚔️ Action Only

###### _Includes_
* Action
* Fights
* Martial Arts
* Murim

#### 💪 Power Fantasy

###### _Includes_
* Genius MC
* Overpowered
* Reincarnation
* Returner
* Regression
* System

#### 🖤 Dark & Gritty

###### _Includes_
* Horror
* Psychological
* Revenge
* Thriller
* Tragedy

#### 🌀 Isekai/Reborn

###### _Includes_
* Isekai
* Rebirth
* Reincarnation
* Regression
* Time Travel

#### 🥋 Martial Arts

###### _Includes_
* Fight
* Martial Arts
* Murim
* Wuxia

#### 🏰 Fantasy World

###### _Includes_
* Demons
* Fantasy
* Magic
* Monsters
* Supernatural
* Vampire

#### 🚫 Skip the Fluff

###### _Excludes_
* Comedy
* Drama
* Ecchi
* Harem
* Josei
* Romance
* School Life
* Shoujo
* Slice of Life

#### 🔍 Mystery/Thriller

###### _Includes_
* Mystery
* Psychological
* Thriller

#### ☠️ Apocalypse

###### _Includes_
* Apocalypse
* Horror
* Monsters
* 


---

```json
{
  "source": "toonverse",
  "data": {
    "items": [
      {
        "id": "2c65dba6-5fc8-4b8d-884d-421fd1fc212c",
        "title": "Solo Leveling",
        "slug": "solo-leveling",
        "coverUrl": "https://cdn.toonverse.net/covers/solo-leveling.webp",
        "author": "Chugong",
        "synopsis": "...",
        "type": "manhwa",
        "status": "ongoing",
        "rating": 5,
        "ratingCount": 6,
        "chapterCount": 205,
        "featured": false,
        "trending": false,
        "isOriginal": false,
        "isAdult": false,
        "createdAt": "2025-12-22T03:19:57.859Z",
        "updatedAt": "2026-07-17T21:04:50.806Z",
        "genres": [
          {
            "id": "3e4ea164-d266-4fdf-9477-69b7e60e819f",
            "name": "Fantasy",
            "slug": "fantasy"
          }
        ],
        "description": "...",
        "viewCount": 2634,
        "chapterReads": 8596
      }
    ],
    "total": 27,
    "limit": 20,
    "offset": 0,
    "hasMore": true
  }
}
```