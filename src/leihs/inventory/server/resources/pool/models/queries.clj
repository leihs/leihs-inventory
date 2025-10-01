(ns leihs.inventory.server.resources.pool.models.queries
  (:require
   [clojure.set]
   [clojure.string :refer [capitalize]]
   [honey.sql.helpers :as sql]
   [hugsql.core :as hugsql]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(hugsql/def-sqlvec-fns "sql/descendent_ids.sql")

(declare descendent-ids-sqlvec)

