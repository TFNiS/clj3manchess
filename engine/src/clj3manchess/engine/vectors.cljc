(ns clj3manchess.engine.vectors
  (:require [schema.core :as s]
            #?(:clj [clojure.spec :as sc]
               :cljs [cljs.spec :as sc])
            [clojure.set :as set]
            [clj3manchess.engine.pos :as p
             :refer [rank file color-segm pos-on-segm same-file same-rank
                     file-dist same-or-opposite-file opposite-file Pos Rank File kfm]]
            [clj3manchess.engine.fig :as f :refer [FigType]]
            [clj3manchess.engine.castling :as cas :refer [CastlingType]]
            [clj3manchess.engine.color :as c]))

(defn one-if-nil-else-input [input] (if (nil? input) 1 input))
(def FileAbs (apply s/enum (range 1 24)))
(def Abs FileAbs)
(def RankAbs (apply s/enum (range 1 12)))
(sc/def ::abs (sc/and int? pos? #(< % 24)))
(sc/def ::inward (sc/nilable boolean?))
(sc/def ::plusfile (sc/nilable boolean?))
(sc/def ::centeronecloser (sc/nilable boolean?))
(sc/def ::pawnlongjump (sc/nilable true?))
(derive ::jumpvectype ::vectype)
(derive ::pawnvectype ::jumpvectype)
(derive ::pawnlongjumpvectype ::pawnvectype)
;;(def PawnLongJumpVec (s/eq :pawnlongjumpvec))
(def PawnLongJumpVec {(s/required-key :pawnlongjump) (s/eq true)})
(defn bno [nono] (identity #(or (not (contains? % nono))
                                (nil? (nono %)))))
(sc/def ::pawnlongjump (sc/and (sc/keys :req-un [::pawnlongjump])
                               (bno :abs) (bno :inward) (bno :plusfile) (bno :centeronecloser) (bno :prom)
                               #(true? (:pawnlongjump %)) (bno :castling)))
(derive ::knightvectype ::jumpvectype)
(def KnightVec {(s/required-key :plusfile)        s/Bool
                (s/required-key :inward)          s/Bool
                (s/required-key :centeronecloser) s/Bool})
(sc/def ::knight (sc/and (sc/keys :req-un [::inward ::plusfile ::centeronecloser])
                         (bno :abs) (bno :pawnlongjump) (bno :prom) (bno :castling)
                         #(boolean? (:inward %)) #(boolean? (:plusfile %)) #(boolean? (:centeronecloser %))))
(derive ::continuousvectype ::vectype)
(derive ::nopromcontinuousvectype ::continuousvectype)
(derive ::multicontinuousvectype ::nopromcontinuousvectype)
(derive ::axisvectype ::continuousvectype)
(derive ::nopromaxisvectype ::axisvectype)
(derive ::nopromaxisvectype ::nopromcontinuousvectype)
(derive ::multiaxisvectype ::multicontinuousvectype)
(derive ::multiaxisvectype ::nopromaxisvectype)
(derive ::filevectype ::axisvectype)
(derive ::nopromfilevectype ::filevectype)
(derive ::nopromfilevectype ::nopromaxisvectype)
(derive ::multifilevectype ::nopromfilevectype)
(derive ::multifilevectype ::multiaxisvectype)
(def FileVec {(s/required-key :plusfile) s/Bool
              (s/optional-key :abs)      FileAbs})
(sc/def ::file (sc/and (sc/keys :req-un [::plusfile] :opt-un [::abs])
                       (bno :inward) (bno :centeronecloser) (bno :pawnlongjump) (bno :prom)
                       #(boolean? (:plusfile %)) (bno :castling)))
(derive ::rankvectype ::axisvectype)
(derive ::nopromrankvectype ::rankvectype)
(derive ::nopromrankvectype ::nopromaxisvectype)
(derive ::multirankvectype ::multiaxisvectype)
(derive ::multirankvectype ::nopromrankvectype)
(def RankVec {(s/required-key :inward) s/Bool
              (s/optional-key :abs)    RankAbs})
(defn rank-friendly-abs [x] (not (and (contains? x :abs) (<= 12 (:abs x)))))
(def more-than-one-abs (sc/and #(contains? % :abs) #(< 1 (:abs %))))
(defn one-just-abs [x] (not (and (contains? x :abs) (< 1 (:abs x)))))
(defn one-if-prom [x] (not (and (contains? x :prom) (not (nil? (:prom x))) (not (one-just-abs x)))))
(sc/def ::prom (sc/nilable f/promfigtypes))
(sc/def ::rank (sc/and (sc/keys :req-un [::inward] :opt-un [::abs ::prom])
                       (bno :plusfile) (bno :centeronecloser) (bno :pawnlongjump) (bno :castling)
                       rank-friendly-abs #(boolean? (:inward %)) one-if-prom))
(derive ::diagvectype ::continuousvectype)
(derive ::nopromdiagvectype ::diagvectype)
(derive ::nopromdiagvectype ::nopromcontinuousvectype)
(derive ::multidiagvectype ::nopromdiagvectype)
(derive ::multidiagvectype ::multicontinuousvectype)
(def DiagVec {(s/required-key :plusfile) s/Bool
              (s/required-key :inward)   s/Bool
              (s/optional-key :abs)      RankAbs})
(sc/def ::diag (sc/and (sc/keys :req-un [::inward ::plusfile] :opt-un [::abs ::prom])
                       (bno :centeronecloser) (bno :pawnlongjump) (bno :castling)
                       rank-friendly-abs #(boolean? (:inward %)) #(boolean? (:plusfile %)) one-if-prom))
(sc/def ::filediag (sc/and (sc/keys :req-un [::plusfile] :opt-un [::abs ::prom ::inward])
                           (bno :centeronecloser) (bno :pawnlongjump) (bno :castling)
                           #(or ((bno :inward) %) (rank-friendly-abs %)) #(boolean? (:plusfile %)) one-if-prom))
(sc/def ::rankdiag (sc/and (sc/keys :req-un [::inward] :opt-un [::abs ::prom ::plusfile])
                           (bno :centeronecloser) (bno :pawnlongjump) rank-friendly-abs one-if-prom
                           #(boolean? (:inward %)) (bno :castling)))
(def AxisVec (s/conditional #(contains? % :plusfile) FileVec #(contains? % :inward) RankVec))
(sc/def ::axis (sc/or :rank ::rank :file ::file))
(def Prom (s/enum :queen :rook :bishop :knight))
(derive ::pawnpromvectype ::pawnvectype)
(derive ::pawnwalkvec ::pawnvectype)
(derive ::pawnwalkvec ::rankvectype)
(derive ::pawnpromwalkvectype ::pawnwalkvec)
(derive ::pawnpromwalkvectype ::pawnpromvectype)
(derive ::pawncapvec ::pawnvectype)
(derive ::pawncapvec ::diagvectype)
(derive ::pawnpromcapvectype ::pawncapvec)
(derive ::pawnpromcapvectype ::pawnpromvectype)
(def PawnWalkVec {(s/required-key :inward) s/Bool
                  (s/optional-key :prom)   Prom})
(sc/def ::pawnwalk (sc/and (sc/keys :req-un [::inward] :opt-un [::prom ::abs])
                           (bno :plusfile) (bno :centeronecloser) (bno :pawnlongjump) (bno :castling)
                           one-just-abs #(boolean? (:inward %))))
(def RankOrPawnWalkVec (s/conditional #(contains? % :abs) RankVec :else PawnWalkVec))
(def PawnCapVec {(s/required-key :inward)   s/Bool
                 (s/required-key :plusfile) s/Bool
                 (s/optional-key :prom)     Prom})
(sc/def ::pawncap (sc/and (sc/keys :req-un [::inward ::plusfile] :opt-un [::prom ::abs])
                          (bno :centeronecloser) (bno :pawnlongjump) one-just-abs
                          #(boolean? (:inward %)) #(boolean? (:plusfile %)) (bno :castling)))
(def DiagOrPawnCapVec (s/conditional #(contains? % :abs) DiagVec :else PawnCapVec))
(def PawnContVec (s/conditional #(not (contains? % :plusfile)) PawnWalkVec :else PawnCapVec))
(sc/def ::pawncont (sc/and (sc/keys :req-un [::inward] :opt-un [::prom ::abs ::plusfile])
                           (bno :centeronecloser) (bno :pawnlongjump) one-just-abs #(boolean? (:inward %))
                           (bno :castling)))
(def PawnPromVec (s/both PawnContVec {(s/required-key :inward) s/Bool
                                      (s/optional-key :plusfile) s/Bool
                                      (s/required-key :prom) Prom}))
(sc/def ::pawnprom (sc/and (sc/keys :req-un [::inward ::prom] :opt-un [::abs ::plusfile])
                           (bno :centeronecloser) (bno :pawnlongjump) (bno :castling) one-just-abs
                           #(boolean? (:inward %)) #(not (nil? (:prom %)))))
(def ContVecNoProm (s/conditional #(not (and (contains? % :inward)
                                             (contains? % :plusfile))) AxisVec :else DiagVec))
(def ContVec (s/conditional #(or (contains? % :abs)
                                 (not (contains? % :inward))) ContVecNoProm :else PawnContVec))
(sc/def ::cont (sc/or :rankdiag ::rankdiag :file ::file))
(s/defn abs :- Abs [vec :- ContVec] (one-if-nil-else-input (:abs vec)))
;(s/def ::multipliedvec (s/and ::multiplicablevec
;                               (s/keys :req-un [::abs]) #(> (:abs %) 1)))
;(s/def ::kingvec (s/and ::contvec #(not (contains? % :abs))))
(def KingRankVec {(s/required-key :inward) s/Bool})
(def KingFileVec {(s/required-key :plusfile) s/Bool})
(def KingDiagVec {(s/required-key :inward)   s/Bool
                  (s/required-key :plusfile) s/Bool})
(def KingAxisVec (s/conditional #(contains? % :inward) KingRankVec #(contains? % :plusfile) KingFileVec))
(def KingContVec (s/conditional #(not (and (contains? % :inward)
                                           (contains? % :plusfile))) KingAxisVec :else KingDiagVec))
(def CastlingVec {(s/required-key :castling) CastlingType})
(sc/def ::castling (sc/and (sc/keys :req-un [::cas/castling]) (bno :inward) (bno :prom) (bno :abs) (bno :plusfile)
                           (bno :centeronecloser) (bno :pawnlongjump)))
;(def KingVec (s/both ContVecNoProm {(s/optional-key :inward) s/Bool
;                                    (s/optional-key :plusfile) s/Bool}))
(def KingVec (s/conditional #(contains? % :castling) CastlingVec :else KingContVec))
;; (s/def ::pawnvec (s/or :pawncontvec (s/and (s/or ::diagvec ::rankvec)
;;                               #(not (contains? % :abs))
;;                               (s/keys :opt-un [::prom]))
;;                        :pawnlongjumpvec ::pawnlongjumpvec))
(def PawnVec (s/conditional map? PawnContVec :else PawnLongJumpVec))
(sc/def ::pawn (sc/or :cont ::pawncont :longjump ::pawnlongjump))
(def Vec (s/conditional (complement map?) PawnLongJumpVec #(contains? % :centeronecloser) KnightVec
                        #(or (contains? % :abs)
                             (contains? % :prom)) ContVec :else KingVec))
(sc/def ::any (sc/or :pawnlongjump ::pawnlongjump
                     :knight ::knight
                     :castling ::castling
                     :cont ::cont))

(s/defn sgn :- (s/enum -1 1) [n :- s/Num] (if (neg? n) -1 1))
(s/defn ?1:0:-1 :- (s/enum -1 0 1) [n :- s/Num] (cond (pos? n) 1
                                                      (zero? n) 0
                                                      (neg? n) -1))

(def castling-file-diff-sgnf {:queenside - :kingside +})
(def castling-file-diff {:queenside -2 :kingside 2})
(def castling-bef-rook-pos {:queenside 0 :kingside 7})
(def castling-empties {:queenside '(3, 2, 1) :kingside '(5, 6)})

(s/defn is-diagvec? :- s/Bool [vec :- Vec] ;; (and (map? vec)
                                           ;;      (every? true?
                                           ;;              (map #(contains? vec %)
                                           ;;                   [:inward :plusfile])))
  (sc/valid? ::diag vec))
(s/defn is-knights? :- s/Bool [vec :- Vec] (sc/valid? ::knight vec)) ;;(and (map? vec) (contains? vec :centeronecloser)))
(s/defn is-rankvec? :- s/Bool [vec :- Vec] ;; (and (map? vec)
                                           ;;      (contains? vec :inward)
                                           ;;      (not (contains? vec :plusfile)))
  (sc/valid? ::rank vec))
(s/defn is-filevec? :- s/Bool [vec :- Vec] ;; (and (map? vec)
                                           ;;      (contains? vec :plusfile)
                                           ;;      (not (contains? vec :inward)))
  (sc/valid? ::file vec))
(s/defn is-axisvec? :- s/Bool [vec :- Vec] ;; (and (map? vec)
                                           ;;      (not= (contains? vec :plusfile)
                                           ;;            (contains? vec :inward)))
  (sc/valid? ::axis vec))
(s/defn is-contvec? :- s/Bool [vec :- Vec] ;; (and (map? vec)
                                           ;;      (or (contains? vec :plusfile)
                                           ;;          (contains? vec :inward)))
  (sc/valid? ::cont vec))
(s/defn is-castvec? :- s/Bool [vec :- Vec] ;; (and (map? vec) (contains? vec :castling))
  (sc/valid? ::castling vec))
(s/defn is-pawnlongjumpvec? :- s/Bool [vec :- Vec] ;; (= vec :pawnlongjump)
  (sc/valid? ::pawnlongjump vec))

(s/defn type-of-cont-vec :- (s/enum :rankvec :filevec :diagvec)
  ([vec :- ContVecNoProm] ;; (cond (is-rankvec? vec) :rankvec
                          ;;       (is-filevec? vec) :filevec
                          ;;       (is-diagvec? vec) :diagvec)
   (key (sc/conform (sc/or :rankvec ::rank :filevec ::file :diagvec ::diag) vec)))
  ([vec :- ContVecNoProm, _] (type-of-cont-vec vec)))

(s/defn thru-center-cont-vec? :- s/Bool
  ([inward :- (s/maybe s/Bool), abs :- FileAbs, from-rank :- Rank]
   (and (not (nil? inward))
        (let [abs (one-if-nil-else-input abs)]
          (and inward (> (+ abs from-rank) 5)))))
  ([vec :- ContVec, from-rank :- Rank]
   (thru-center-cont-vec? (:inward vec) (:abs vec) from-rank)))
(sc/fdef thru-center-cont-vec?
         :args (sc/or :destructured (sc/cat :inward ::inward :abs ::abs :from-rank ::p/rank)
                      :structured (sc/cat :vec ::cont :from-rank ::p/rank))
         :ret boolean?)

(s/defn ranks-inward-to-pass-center :- (s/enum 1 2 3 4 5 6)
  [from-rank :- Rank] (inc (- 5 from-rank)))
(sc/fdef ranks-inward-to-pass-center :args (sc/cat :from-rank ::p/rank) :ret #{1 2 3 4 5 6})

(def one-inward {:inward true})
(def one-outward {:inward false})

(s/defn units-rank-vec :- [(s/one KingRankVec "first required") KingRankVec]
  ([vec :- RankVec, from-rank :- Rank] (units-rank-vec (:inward vec) (:abs vec) from-rank))
  ([inward :- s/Bool, abs :- RankAbs, from-rank :- Rank]
   (let [abs (one-if-nil-else-input abs)]
     (if (thru-center-cont-vec? inward abs from-rank)
       (concat (repeat (ranks-inward-to-pass-center from-rank) one-inward)
               (repeat (- abs (ranks-inward-to-pass-center from-rank)) one-outward))
       (repeat abs {:inward inward})))))
(sc/fdef units-rank-vec
         :args (sc/or :destructured (sc/cat :inward ::inward :abs ::abs :from-rank ::p/rank)
                      :structured (sc/cat :vec ::rank :from-rank ::p/rank))
         :ret (sc/coll-of (sc/and ::rank one-just-abs) :kind seq? :min-count 1 :max-count 11 :distinct false))

(s/defn units-file-vec :- [(s/one KingFileVec "first required") KingFileVec]
  ([vec :- FileVec] (units-file-vec (:abs vec) (:plusfile vec)))
  ([abs :- FileAbs, plusfile :- s/Bool] (cond (not (nil? plusfile))
                                              (repeat (one-if-nil-else-input abs)
                                                      {:plusfile plusfile}))))
(sc/fdef units-file-vec
         :args (sc/or :destructured (sc/cat :abs ::abs :plusfile ::plusfile)
                      :structured (sc/cat :vec ::file))
         :ret (sc/coll-of (sc/and ::file one-just-abs) :kind seq? :min-count 1 :max-count 23 :distinct false))

(s/defn units-diag-vec :- [(s/one KingDiagVec "first required") KingDiagVec]
  ([vec :- DiagVec, from-rank :- Rank] (units-diag-vec (:inward vec) (:plusfile vec)
                                                       (:abs vec) from-rank))
  ([inward :- s/Bool, plusfile :- s/Bool, abs :- RankAbs, from-rank :- Rank]
   (let [abs (one-if-nil-else-input abs)]
     (if-not inward (repeat abs {:inward inward :plusfile plusfile})
             (if (thru-center-cont-vec? inward abs from-rank)
               (concat
                (repeat (ranks-inward-to-pass-center from-rank)
                        {:inward inward :plusfile plusfile})
                (repeat (- abs (ranks-inward-to-pass-center from-rank))
                        {:inward false :plusfile (not plusfile)}))
               (repeat abs {:inward inward :plusfile plusfile}))))))
(sc/fdef units-diag-vec
         :args (sc/or :descructured (sc/cat :inward (sc/and ::inward boolean?)
                                            :plusfile (sc/and ::plusfile boolean?)
                                            :abs ::abs :from-rank ::p/rank)
                      :structured (sc/cat :vec ::diag :from-rank ::p/rank))
         :ret (sc/coll-of (sc/and ::diag one-just-abs) :kind seq? :min-count 1 :max-count 11 :distinct false))

;; (defn units [vec from-rank] (cond (contains? vec :abs)
;;                                       (cond
;;                                         (s/valid? ::diagvec vec) (units-diag-vec vec from-rank)
;;                                         (s/valid? ::filevec vec) (units-file-vec vec)
;;                                         (s/valid? ::rankvec vec) #{:inward :abs}) (units-rank-vec vec from-rank)
;;                                         :else (throw (IllegalArgumentException.))) ;; maybe return vec?
;;                                       :else '(vec))

(defmulti units type-of-cont-vec)

(s/defmethod units :diagvec :- [(s/one KingDiagVec "first required") KingDiagVec] ;([vec :- DiagVec] (fn [from-rank] (units-diag-vec vec from-rank)))
  [vec :- DiagVec, from-rank :- Rank] (units-diag-vec vec from-rank))

(s/defmethod units :filevec :- [(s/one KingFileVec "first required") KingFileVec]
  ([vec :- FileVec] (units-file-vec vec)) ([vec :- FileVec, _] (units-file-vec vec)))

(s/defmethod units :rankvec :- [(s/one KingRankVec "first required") KingRankVec] ;;([vec] (fn [from-rank] (units-rank-vec vec from-rank)))
  [vec :- RankVec, from-rank :- RankVec] (units-rank-vec vec from-rank))

(def sgnb {true 1 false -1})
(def sgnb*2 {true 2 false -2})
(def ?1:-2 {true 1 false -2})
(def ?1:0 {true 1 false 0})
(def ?2:1 {true 2 false 1})
(def sgnf {true + false -})

(s/defn thru-center-knight-vec? :- s/Bool
  ([vec :- KnightVec, from-rank :- Rank]
   (thru-center-knight-vec? (:inward vec) (:centeronecloser vec) from-rank))
  ([inward :- s/Bool, centeronecloser :- s/Bool, from-rank :- Rank]
   (and inward (or (and centeronecloser (>= from-rank 4))
                   (= from-rank 5)))))

(s/defn add-knight-vec :- Pos [vec :- KnightVec, from :- Pos]
  (let [{:keys [inward plusfile centeronecloser]} vec
        from-rank (rank from)
        from-file (file from)
        more-rank (= centeronecloser inward)
        absrank (?2:1 more-rank)
        more-file (not more-rank)
        absfile (?2:1 more-file)
        rank-mov ((sgnf inward) absrank)
        file-mov ((sgnf plusfile) absfile)
        rank-to (+ from-rank rank-mov)
        thru-pass (> rank-to 5)
        rank-to (if-not thru-pass rank-to (- 6 (- rank-to 5)))
        file-to (+ from-file file-mov)
        file-to (if thru-pass (+ file-to 12) file-to)]
    [rank-to (mod file-to 24)]))
    ;; (cond (thru-center-knight-vec? inward centeronecloser from-rank)
    ;;       (cond centeronecloser
    ;;             [(- (+ 5 4) from-rank)
    ;;              (mod (+ from-file (sgnb plusfile) 12) 24)]
    ;;             :else
    ;;             [5 (mod (+ from-file (sgnb*2 plusfile) 12) 24)])
    ;;       :else
    ;;       [(+ from-rank (?1:-2 plusfile) (?1:0 centeronecloser))
    ;;        (mod (+ from-file (* (?2:1 (not= centeronecloser inward))
    ;;                             (sgnb plusfile))) 24)])))

(s/defn add-castling-vec :- Pos
  [vec :- CastlingVec, from :- Pos] (when (and (zero? (rank from)) (= (mod (file from) 8) kfm))
                                      [0 (+ (file from) (castling-file-diff (:castling vec)))]))

(s/defn add-pawnlongjumpvec :- Pos ([from :- Pos] (when (= (rank from) 1) [3 (file from)]))
  ([_ :- PawnLongJumpVec, from :- Pos] (add-pawnlongjumpvec from)))

(s/defn add-short-diag :- Pos
  ([vec :- DiagVec, from :- Pos]
   (add-short-diag (:inward vec) (:plusfile vec) (abs vec) from))
  ([inward :- s/Bool, plusfile :- s/Bool, abs :- RankAbs, from :- Pos]
   (add-short-diag inward plusfile abs (rank from) (file from)))
  ([inward :- s/Bool, plusfile :- s/Bool, abs :- RankAbs,
    rank-from :- p/Rank, file-from :- p/File]
   (let [to-rank ((if inward + -) rank-from abs)]
     (when (>= 5 to-rank 0) [to-rank (mod ((if plusfile + -)
                                           file-from abs) 24)]))))

(s/defn add-solely-thru-center-diag-file :- File [plusfile :- s/Bool, file-from :- p/File]
  (mod (+ file-from
          (if plusfile -10 10))
       24))

(s/defn diag-split-vecs :- (s/conditional #(not (contains? % :solely-thru-center))
                                          {(s/required-key :short-upto-rank5) DiagVec} :else
                                          {(s/optional-key :short-upto-rank5)      DiagVec
                                           (s/required-key :solely-thru-center)    KingDiagVec
                                           (s/optional-key :short-past-the-center) DiagVec})
  ([vec :- DiagVec, rank-from :- p/Rank]
   (diag-split-vecs (:inward vec)
                    (:plusfile vec)
                    (abs vec)
                    rank-from))
  ([inward :- s/Bool, plusfile :- s/Bool, abs :- RankAbs, rank-from :- p/Rank]
   (if inward (diag-split-vecs plusfile abs rank-from)
       {:short-upto-rank5
        (when (>= rank-from abs) {:inward false, :plusfile plusfile, :abs abs})}))
  ([plusfile :- s/Bool, abs :- RankAbs, rank-from :- p/Rank]
   (let [from-plus-abs (+ abs rank-from)
         further (- from-plus-abs 6)]
     (cond (neg? further)
           {:short-upto-rank5 {:inward true :plusfile plusfile :abs abs}}
           (zero? further)
           {:short-upto-rank5   {:inward true :plusfile plusfile :abs (- abs 1)}
            :solely-thru-center {:inward true :plusfile plusfile}}
           (pos? further)
           {:short-upto-rank5      {:inward true :plusfile plusfile
                                    :abs    (- abs (inc further))}
            :solely-thru-center    {:inward true :plusfile plusfile}
            :short-past-the-center {:inward false :plusfile (not plusfile)
                                    :abs    further}}))))

(s/defn add-unit-diag-vec :- Pos [plusfile :- s/Bool, inward :- s/Bool, from :- Pos]
  (if (and (= (rank from) 5) inward)
    [5 (add-solely-thru-center-diag-file plusfile (file from))]
    (add-short-diag inward plusfile 1 from)))

(s/defn add-diag-vec :- Pos [vec :- DiagOrPawnCapVec, from :- Pos]
  ;(let [abs (abs vec)
  ;      {:keys [inward plusfile]} vec]
  ;  ((if inward
  ;     add-diag-inward
  ;     add-diag-outward) plusfile abs from))
  (let [abs (abs vec)
        {:keys [inward plusfile]} vec]
    (if (= 1 abs)
      (add-unit-diag-vec plusfile inward from)
      (let [split (diag-split-vecs inward plusfile abs (rank from))
            {:keys
             [short-upto-rank5 solely-thru-center short-past-the-center]} split]
        (((comp (partial apply comp) reverse)
          [#(if (nil? short-upto-rank5) % (add-short-diag short-upto-rank5 %))
           #(if (nil? solely-thru-center) % [5 (add-solely-thru-center-diag-file
                                                (:plusfile solely-thru-center) (file %))])
           #(if (nil? short-past-the-center) % (add-short-diag short-past-the-center %))])
         from)))))

(s/defn add-rank-vec :- Pos [vec :- RankOrPawnWalkVec, from :- Pos]
  (let [abs (abs vec)] (cond (:inward vec)
                             (let [to-rank-dir (+ (rank from) abs)]
                               (if (> to-rank-dir 5)
                                 [(- 11 to-rank-dir) (mod (+ (file from) 12) 24)]
                                 [to-rank-dir (file from)]))
                             :else (let [to-rank (- (rank from) abs)]
                                     (if-not (sc/valid? ::p/rank to-rank) ::addition-error
                                             [to-rank (file from)])))))

(s/defn add-file-vec :- Pos [vec :- FileVec, from :- Pos]
  (let [abs (abs vec)]
    [(rank from) (mod ((cond (:plusfile vec) + :else -)
                       (file from) abs) 24)]))

;addvec returns nil in place of VectorAdditionFailedException
;;but how to represent it with schema?
(sc/def ::addvec-ret (sc/or :pos ::p/pos :err ::addition-err))
(s/defn addvec :- (s/maybe Pos)
  ([vec :- Vec, from :- Pos] (let [res (cond
                                         (is-knights? vec) (add-knight-vec vec from)
                                         (is-castvec? vec) (add-castling-vec vec from)
                                         (is-pawnlongjumpvec? vec) (add-pawnlongjumpvec from)
                                         (is-diagvec? vec) (add-diag-vec vec from)
                                         (is-rankvec? vec) (add-rank-vec vec from)
                                         (is-filevec? vec) (add-file-vec vec from))]
                               (if (sc/valid? ::addvec-ret res) res ::addition-error)))
  ([bv] (addvec bv (:from bv))))
(sc/def ::bound (sc/and (sc/keys :req-un [::from]) ::any #(sc/valid? ::p/pos (addvec %))))
(sc/def ::addition-err #{::addition-error})
(sc/fdef addvec :args (sc/or :bv (sc/cat :bv ::bound)
                             :destr (sc/cat :vec ::any :from ::p/pos)) :ret ::addvec-ret)
(def +v addvec)

(s/defn destinations-of-a-sequence-of-vecs :- [Pos]
  [units-seq :- [Vec], fromp :- Pos]
  ;(if (= 1 (count units)) [(addvec (first units) from)]
  ;                        (recur (rest units) (addvec (first units) from))))
  (if (empty? units-seq)
    []
    (loop [prev [fromp], left (vec units-seq)]
      (if (= 1 (count left)) (conj (rest prev) (addvec (first left) (last prev)))
          (recur
           (conj prev (addvec (first left) (last prev)))
           (rest left))))))

(s/defn empties-cont-vec :- [Pos] [vec :- ContVecNoProm, from :- Pos]
  (let [units (units vec (rank from))]
    (destinations-of-a-sequence-of-vecs
     (butlast units)
     from)))

;; (def BoundVec {(s/required-key :from) Pos
;;                (s/required-key :vec)  Vec})
(sc/def ::from ::p/pos)
(s/defn bv-to :- Pos [bv] (addvec bv))
(sc/fdef bv-to :args (sc/cat :bv ::bound) :ret ::addvec-ret)

(comment :addition-error)

(s/defn moat-diag-vec :- (s/maybe c/Color) [from :- Pos, to :- Pos, plusfile :- s/Bool]
  (cond (zero? (rank from))
        (when plusfile
          (case (mod (file from) 8)
            7 (p/color-segm from)
            0 (c/prev-col (p/color-segm from))
            nil))
        (zero? (rank to))
        (when-not plusfile
          (case (mod (file to) 8)
            7 (p/color-segm to)
            0 (c/prev-col (p/color-segm to))
            nil))))
(s/defn moats-file-vec :- [c/Color] [from :- Pos, abs :- FileAbs, plusfile :- s/Bool]
  (when (zero? (rank from))
    (let [start   (p/color-segm from)
          from    (file from)
          left    (mod from 8)
          tm      (if plusfile (- 7 left) left)
          moating (- abs tm)
          li      (if plusfile
                    [(c/next-col start) (c/prev-col start) start]
                    [start              (c/prev-col start) (c/next-col start)])
          >0-8-16 (if-not (> moating 0)
                    0 (if-not (> moating 8)
                        1 (if-not (> moating 16)
                            2 3)))]
      (take >0-8-16 li))))
(def xrqnmv {#{6 0} 1
             #{7 1} 1
             #{7 0} 2})
(s/defn moat-knight-vec :- (s/maybe c/Color) [from :- Pos, to :- Pos]
  (let [xoreq (when-not (and (> (rank from) 2) (> (rank to) 2))
                (let [ffm (mod (file from) 8)
                      tfm (mod (file to) 8)
                      fms #{ffm tfm}
                      w (xrqnmv fms)]
                  (= #{(rank from) (rank to)}
                     #{0 w})))]
    (when (true? xoreq) (get c/colors (-> from (file) (+ 2) (quot 8) (mod 3))))))

(s/defn tfmapset :- #{{(s/one s/Keyword "some keyword, but just one") s/Bool}}
  [keyword :- s/Keyword] #{{keyword true} {keyword false}})

(s/defn creek :- s/Bool
  ([from :- Pos, vec :- PawnCapVec] (and (< (rank from) 3)
                                         (or (and (:plusfile vec) (= (mod (file from) 8) 7))
                                             (and (not (:plusfile vec)) (= mod (file from) 8) 0))))
  ([{from :from :as vec}] (creek (from vec))))

(defn wrappedfilevec ([t f wlong]
                                   (let [diff (- t f)
                                         sgnf (if (neg? diff) - +)]
                                     (if (= wlong (< 12 (sgnf diff)))
                                       diff
                                       (- diff (sgnf 24)))))
  ([t f] (wrappedfilevec t f false)))

(defn castling-vecft [f t]
  (cond (and (= (rank f) 0 (rank t))
             (= (mod (file f) 8) kfm))
        (case (mod (file t) 8)
          2 {:castling :queenside}
          6 {:castling :kingside}
          nil)))
(sc/fdef castling-vecft :args (sc/cat :f ::from :t ::to) :ret (sc/nilable ::castling))
(defn pawnwalk-vecft [f t]
  (first (filter #(= t (addvec % f))
                 (tfmapset :inward))))
(sc/fdef pawnwalk-vecft :args (sc/cat :f ::from :t ::to) :ret (sc/nilable (sc/and ::pawnwalk (bno :prom))))
(defn pawnlongjump-vecft [f t]
  (when (and (= (rank f) 1) (= (rank t) 3)
             (= (file f) (file t))) ;; :pawnlongjump
        {:pawnlongjump true}))
(sc/fdef pawnlongjump-vecft :args (sc/cat :f ::from :t ::to) :ret (sc/nilable ::pawnlongjump))
(defn pawncap-vecft [f t]
  (first (filter #(and (not (creek f %))
                       (= t (addvec % f)))
                 (set/join (tfmapset :inward)
                           (tfmapset :plusfile)))))
(sc/fdef pawncap-vecft :args (sc/cat :f ::from :t ::to) :ret (sc/nilable (sc/and ::pawncap (bno :prom))))
(defn pawn-vecft [f t]
  (first (filter (complement nil?) [(pawnwalk-vecft f t)
                                    (pawnlongjump-vecft f t)
                                    (pawncap-vecft f t)])))
(sc/fdef pawn-vecft :args (sc/cat :f ::from :t ::to) :ret (sc/nilable (sc/and ::pawn (bno :prom))))
(defn rank-vecft [from to]
  (cond (same-or-opposite-file from to)
        (let [t (if (same-file from to)
                  (- (rank to) (rank from))
                  (- 11 (+ (rank from) (rank to))))
              inward (< 0 t)
              abs (if inward t (- t))]
          (cond (= 1 abs) {:inward inward}
                (not (= 0 abs))
                {:inward inward :abs abs}))))
(sc/fdef rank-vecft :args (sc/cat :from ::from :to ::to) :ret (sc/nilable (sc/and ::rank (bno :prom))))
(defn file-vecft [from to]
  (not-empty (vec (filter (complement nil?)
                      (sort-by :abs
                               (let [diff (- (file to) (file from))
                                     plusfile (< 0 diff)
                                     absdiff (if plusfile diff (- diff))]
                                 (when (and (= (rank to) (rank from))
                                            (not (= 0 absdiff))
                                            (> 24 absdiff))
                                   [{:plusfile plusfile
                                     :abs absdiff}
                                    {:plusfile (not plusfile)
                                     :abs (- 24 absdiff)}])))))))
(sc/fdef file-vecft :args (sc/cat :from ::from :to ::to) :ret (sc/nilable (sc/tuple (sc/and ::file #(<= (:abs %) 12))
                                                                                   (sc/and ::file #(>= (:abs %) 12)))))
(defn knight-vecft [fromp top]
  (first
   (filter #(= top (addvec % fromp))
           (set/join (set/join (tfmapset :inward)
                               (tfmapset :plusfile))
                     (tfmapset :centeronecloser)))))
(sc/fdef knight-vecft :args (sc/cat :fromp ::from :top ::to) :ret (sc/nilable ::knight))
(defn kingcont-vecft [from to]
  (first
   (filter #(= to (addvec % from))
           (set/union (tfmapset :inward)
                      (tfmapset :plusfile)
                      (set/join (tfmapset :inward)
                                (tfmapset :plusfile))))))
(sc/fdef kingcont-vecft :args (sc/cat :from ::from :to ::to) :ret (sc/nilable (sc/and ::cont (bno :prom) one-just-abs)))
(defn king-vecft [from to]
  (first (filter (complement nil?)
                 [(castling-vecft from to)
                  (kingcont-vecft from to)])))
(sc/fdef king-vecft :args (sc/cat :from ::from :to ::to) :ret (sc/nilable
                                                              (sc/or :cont (sc/and ::cont (bno :prom) one-just-abs)
                                                                     :cast ::castling)))
(defn diag-vecft [from to]
  (let [filediff (wrappedfilevec (file from) (file to))
        plusfile (> filediff 0)
        absfilediff (if plusfile filediff (- filediff))
        inwardshort (> (rank to) (rank from))
        absrankdiff (if inwardshort
                      (- (rank to) (rank from))
                      (- (rank from) (rank to)))
        ser (when (and (not (= 0 absrankdiff))
                       (= absfilediff absrankdiff))
              {:abs absfilediff
               :inward inwardshort
               :plusfile plusfile})
        ranksum (+ (rank to) (rank from))
        ler (when (and (not (= 0 absfilediff))
                       (= absfilediff ranksum))
              {:abs    (- (+ 5 5 1) ranksum) ;; (5-s)+1+(5-r)
               :inward true
               :plusfile (not plusfile)})]
    (filter (complement nil?) [ser ler])))
(sc/fdef diag-vecft :args (sc/cat :from ::from :to ::to)
         :ret (sc/coll-of (sc/and ::diag (bno :prom)) :kind sequential? :max-count 2 :distinct true))
(defn axis-vecft [from to]
  (set/union (set/select (complement nil?)
                         #{(rank-vecft from to)})
             (set (file-vecft from to))))
(defn cont-vecft [from to]
  (set/union (axis-vecft from to)
             (set (diag-vecft from to))))

(def vecftset {
               ::axisvec axis-vecft
               ::castlingvec #(set/select (complement nil?) (castling-vecft %1 %2))
               ::contvec cont-vecft
               ::pawnwalkvec #(set/select (complement nil?) (pawnwalk-vecft %1 %2))
               ::pawnlongjumpvec #(set/select (complement nil?) (pawnlongjump-vecft %1 %2))
               ::pawncapvec #(set/select (complement nil?) (pawncap-vecft %1 %2))
               ::pawnvec #(set/select (complement nil?) (pawn-vecft %1 %2))
               ::rankvec #(set/select (complement nil?) (rank-vecft %1 %2))
               ::filevec (comp set file-vecft)
               ::knightvec #(set/select (complement nil?) (knight-vecft %1 %2))
               ::kingcontvec #(set/select (complement nil?) (kingcont-vecft %1 %2))
               ::kingvec #(set/select (complement nil?) (king-vecft %1 %2))
               ::diagvec (comp set diag-vecft)
               })

(def vecft {::axisvec         axis-vecft
            ::castlingvec     castling-vecft
            ::contvec         cont-vecft
            ::pawnwalkvec     pawnwalk-vecft
            ::pawnlongjumpvec pawnlongjump-vecft
            ::pawncapvec      pawncap-vecft
            ::pawnvec         pawn-vecft
            ::rankvec         rank-vecft
            ::filevec         file-vecft
            ::knightvec       knight-vecft
            ::kingcontvec     kingcont-vecft
            ::kingvec         king-vecft
            ::diagvec         diag-vecft})

(def tvec {:pawn   ::pawnvec
           :rook   ::axisvec
           :knight ::knightvec
           :bishop ::diagvec
           :queen  ::contvec
           :king   ::kingvec})
