Require Import Coq.Arith.Arith.
Require Import Coq.Bool.Bool.
Require Import Coq.Strings.String.
Require Import Coq.Arith.EqNat.
Require Import Coq.omega.Omega.
Require Import Coq.Lists.List.

(* a set of machine types *)
Inductive mType: Set := mNone | mUnit | mBool | mNat.

(* a set of real-world types *)
Inductive rType: Set := rNone | rApples | rOranges | rHappy | rSad.

(* function that gets the machine representation of each real-world type *)
Definition rType2MType (r: rType): mType :=
  match r with
    | rApples => mNat
    | rOranges => mNat
    | rHappy => mBool
    | rSad => mBool
    | rNone => mNone
  end.

(* type for identifiers *)
Inductive id : Type :=
  | Id : string -> id.

(* comparison of identifiers *)
Definition beq_id x y :=
  match x,y with
    | Id n1, Id n2 => if string_dec n1 n2 then true else false
  end.

Definition typeInfo : Type := mType * rType.
Notation "( x , y )" := (pair x y).
Check (mNone, rNone).

(* tuple that contains an identifier along with its machine type and real-world type*)
Inductive typingTuple : Type := 
  | tuple : id -> typeInfo -> typingTuple.

Check typeInfo.
Check tuple (Id "jian") (mNone, rNone).

(* get identifer of the typing tuple *)
Definition getId (t : typingTuple) : id :=
  match t with
  | tuple i typeInfo => i
  end.

(* get machine type of the typing tuple *)
Definition getmType (tuple : typingTuple) : mType :=
  match tuple with
  | tuple i typeInfo => (fst typeInfo)
  end.

(* get real-world type of the typing tuple *)
Definition getrType (tuple : typingTuple) : rType :=
  match tuple with
  | tuple i typeInfo => (snd typeInfo)
  end.

(* environment for the type system from identifier to (machine type, real-world type) *)
(* Definition context := list typingTuple. *)
Definition context := list typingTuple.

(* empty context *)
Definition emptyEnv := nil (A:=typingTuple).

(* check if the identifier has been defined in the context *)
Fixpoint idDefined (cont : context) (identifier : id) : bool := 
  match cont with 
  | nil => false
  | h :: t => if (beq_id identifier (getId h)) then true else idDefined t identifier
  end.

(* return the typingTuple of the input identifier *)
Fixpoint getTypeById (cont : context) (identifier : id) : typingTuple := 
  match cont with 
  | nil => tuple identifier (mNone, rNone)
  | h :: t => if (beq_id identifier (getId h)) then h else getTypeById t identifier
  end.

Definition update (ctx : context) (identifier : id) (t : typeInfo) := 
  if (idDefined ctx identifier) then ctx else cons (tuple identifier t) ctx.

Definition tupleToPair (tuple : typingTuple) : typeInfo :=
  pair (getmType tuple) (getrType tuple).

Notation "( x , y )" := (pair x y).

Inductive ty : Type :=
  | termTy : typeInfo -> ty
  | funcTy : ty -> ty -> ty
  | prodTy : ty -> ty -> ty
  | listTy : ty -> ty
  | vDecTy : ty
  | cmdTy : ty
  | paraTy : ty
  | arguTy : ty.


Inductive tm : Type :=
  | Tvar : id -> tm
  (* arithematic *)
  | ANum : nat -> tm 
  | APlus : tm -> tm -> tm
  | AMinus : tm -> tm -> tm
  | AMult : tm -> tm -> tm
  (* boolean expressions *)
  | BTrue : tm
  | BFalse : tm
  | BEq : tm -> tm -> tm
  | BLe : tm -> tm -> tm
  | BNot : tm -> tm
  | BAnd : tm -> tm -> tm
  (* commands *)
  | CSkip : tm
  | CAss : id -> tm -> tm
  | CSeq : tm -> tm -> tm
  | CIf : tm -> tm -> tm -> tm
  | CWhile : tm -> tm -> tm
  (* variable declaration *)
  | TVal_decl : ty -> id -> tm

  (* function declaration *)
  | TDeclFun : id -> tm -> tm -> ty -> tm
(*
  (* lists *)
  | Tnil : ty -> tm
  | Tcons : tm -> tm -> tm
*)
  (* parameters *)
  | Para_nil : tm
  | Para_list : tm -> tm -> tm

  (* arguments *)
  | Argu_nil : tm
  | Argu_list : tm -> tm -> tm

  (* function call*)
  | TFun_call : tm -> tm -> tm
  (* return statement*)
  | TFun_return : tm -> tm.

(* substitution function for term tm *)
Reserved Notation "'[' x ':=' s ']' t" (at level 20).

Fixpoint subst (x:id) (s:tm) (t:tm) : tm :=
  match t with
  | Tvar y =>
      if beq_id x y then s else t
  | ANum n =>
      ANum n
  | APlus t1 t2 =>
      APlus (subst x s t1) (subst x s t2)
  | AMinus t1 t2 =>
      AMinus (subst x s t1) (subst x s t2)
  | AMult t1 t2 =>
      AMult (subst x s t1) (subst x s t2)
  | BTrue => BTrue
  | BFalse => BFalse
  | BEq t1 t2 =>
     BEq (subst x s t1) (subst x s t2)
  | BLe t1 t2 =>
     BLe (subst x s t1) (subst x s t2)
  | BNot t =>
     BNot (subst x s t)
  | BAnd t1 t2 =>
     BAnd (subst x s t1) (subst x s t2)
  | CSkip => CSkip
  | CAss idf t =>
     CAss idf (subst x s t)
  | CSeq t1 t2 =>
     CSeq (subst x s t1) (subst x s t2)
  | CIf t1 t2 t3 =>
     CIf (subst x s t1) (subst x s t2) (subst x s t3)
  | CWhile t1 t2 =>
     CWhile (subst x s t1) (subst x s t2)
  | TVal_decl t idf => 
     TVal_decl t idf
  | TDeclFun idf t1 t2 T =>
     TDeclFun idf (subst x s t1) (subst x s t2) T
(*
  | Tnil T => Tnil T
  | Tcons t1 t2 =>
      Tcons (subst x s t1) (subst x s t2)
*)
  | TFun_call t1 t2 =>
      TFun_call (subst x s t1) (subst x s t2)
  | TFun_return t =>
      TFun_return (subst x s t)

  | Para_nil => Para_nil
  | Para_list t1 t2 =>
      Para_list (subst x s t1) (subst x s t2)

  | Argu_nil => Argu_nil
  | Argu_list t1 t2 =>
      Argu_list (subst x s t1) (subst x s t2)

  end.

Fixpoint substArgus (params : tm) (args : tm) (e_fun : tm) : tm := 
  match params with 
    | Para_nil => e_fun
    | Para_list t1 t2 => match t1 with
                      | TVal_decl decTy idf => match args with 
                                              | Argu_list t1' t2' => (let body:=(subst idf t1' e_fun) in (substArgus t2 t2' body))
                                              | _ => e_fun
                                              end
                      | _ => e_fun
                      end
    | _ => e_fun
  end.


Reserved Notation "Gamma '|-' t '\in' T" (at level 40).

Inductive has_type : context -> tm -> ty -> Prop :=
  (* typing judgement : watch this one*)
  | TR_Var : forall ctx x,
      tupleToPair (getTypeById ctx x) <> (mNone, rNone) ->
      ctx |- (Tvar x) \in termTy (tupleToPair (getTypeById ctx x))
  (* boolean values *)
  | TR_BTrue : forall ctx,
      ctx |- BTrue \in termTy (mBool, rNone)
  | TR_BFalse : forall ctx,
      ctx |- BFalse \in termTy (mBool, rNone)
  (* arithmetic *)
  | TR_Plus : forall ctx e1 e2,
      ctx |- e1 \in termTy (mNat, rNone) -> 
      ctx |- e2 \in termTy (mNat, rNone) ->
      ctx |- APlus e1 e2 \in termTy (mNat, rNone)
  | TR_Minus : forall ctx e1 e2,
      ctx |- e1 \in termTy (mNat, rNone) -> 
      ctx |- e2 \in termTy (mNat, rNone) ->
      ctx |- AMinus e1 e2 \in termTy (mNat, rNone)
  | TR_Mult : forall ctx e1 e2,
      ctx |- e1 \in termTy (mNat, rNone) -> 
      ctx |- e2 \in termTy (mNat, rNone) ->
      ctx |- AMult e1 e2 \in termTy (mNat, rNone)
  (* numbers *)
  | TR_Num : forall ctx n,
      ctx |- ANum n \in termTy (mNat, rNone)
  (* boolean expression *)
  | TR_BEq : forall ctx e1 e2, 
      ctx |- e1 \in termTy (mNat, rNone) -> 
      ctx |- e2 \in termTy (mNat, rNone) ->
      ctx |- BEq e1 e2 \in termTy (mBool, rNone)
  | TR_BLe : forall ctx e1 e2, 
      ctx |- e1 \in termTy (mNat, rNone) -> 
      ctx |- e2 \in termTy (mNat, rNone) ->
      ctx |- BLe e1 e2 \in termTy (mBool, rNone)
  | TR_BNot : forall ctx e, 
      ctx |- e \in termTy (mBool, rNone) ->
      ctx |- BNot e \in termTy (mBool, rNone)
  | TR_BAnd : forall ctx e1 e2, 
      ctx |- e1 \in termTy (mBool, rNone) -> 
      ctx |- e2 \in termTy (mBool, rNone) ->
      ctx |- BAnd e1 e2 \in termTy (mBool, rNone)
  (* commands *)
  | TR_CSkip : forall ctx, 
      ctx |- CSkip \in cmdTy
  (* command sequence *)
  | TR_CSeq : forall ctx sq1 s, 
      ctx |- sq1 \in cmdTy -> 
      ctx |- s \in cmdTy ->
      ctx |- CSeq sq1 s \in cmdTy

  (* if statment *)
  | TR_CIfcondition : forall ctx b c1 c2,
      ctx |- b \in termTy (mBool, rNone) ->
      ctx |- c1 \in cmdTy -> 
      ctx |- c2 \in cmdTy -> 
      ctx |- CIf b c1 c2 \in cmdTy

  (* while loop *)
  | TR_CWhile : forall ctx e c,
      ctx |- e \in termTy (mBool, rNone) -> 
      ctx |- c \in cmdTy ->
      ctx |- CWhile e c \in cmdTy
  (* assignment *)
  | TR_CAss1 : forall ctx x e T, 
      ctx |- Tvar x \in termTy (T, rNone) -> 
      ctx |- e \in termTy (T, rNone) ->
      ctx |- CAss x e \in cmdTy
  | TR_CAss2 : forall ctx x e T rt, 
      ctx |- Tvar x \in termTy (T, rt) -> 
      ctx |- e \in termTy (T, rNone) ->
      ctx |- CAss x e \in cmdTy
  | TR_CAss3 : forall ctx x e T rt, 
      ctx |- Tvar x \in termTy (T, rNone) -> 
      ctx |- e \in termTy (T, rt) ->
      ctx |- CAss x e \in cmdTy
  | TR_CAss4 : forall ctx x e T rt1 rt2 , 
      ctx |- Tvar x \in termTy (T, rt1) -> 
      ctx |- e \in termTy (T, rt2) ->
      rt1 = rt2 ->
      ctx |- CAss x e \in cmdTy
  (* variable declaration *)
  | TR_ValDecl : forall ctx t idf, 
      ctx |- Tvar idf \in t ->
      ctx |- (TVal_decl t idf) \in vDecTy
(*
  (* lists *)
  | TR_Nil : forall ctx T,
      ctx |- (Tnil T) \in (listTy T)
  | TR_Cons : forall ctx t1 t2 T1,
      ctx |- t1 \in T1 ->
      ctx |- t2 \in (listTy T1) ->
      ctx |- (Tcons t1 t2) \in (listTy T1)
*)

  (* parameters *)
  | TR_para_nil : forall ctx,
      ctx |- Para_nil \in termTy (mNone, rNone)
  | TR_para_list : forall ctx t1 t2 idf decTy T1 T2,
      t1 = TVal_decl decTy idf ->
      ctx |- (Tvar idf) \in T1 ->
      ctx |- t2 \in T2 ->
      ctx |- (Para_list t1 t2) \in (prodTy T1 T2)

  (* arguments *)
  | TR_argu_nil : forall ctx, 
      ctx |- Argu_nil \in termTy (mNone, rNone) 
  | TR_argu_list : forall ctx t1 t2 T1 T2,
      ctx |- t1 \in T1 -> 
      ctx |- t2 \in T2 -> 
      ctx |- (Argu_list t1 t2) \in (prodTy T1 T2)


  (* function declaration *)
  | TR_DeclFun : forall ctx idf params paraTypes block bodyTy reType,
      ctx |- block \in bodyTy ->
      ctx |- params \in paraTypes ->
      idDefined ctx idf = false ->
      reType = bodyTy ->
      ctx |- (TDeclFun idf params block reType) \in (funcTy paraTypes reType)
  (* function return value *)
  | TR_funreturn : forall ctx e T,
      ctx |- e \in T ->
      ctx |- (TFun_return e) \in cmdTy


  (* function calls *)
  | TR_funcall1 : forall ctx idf params block argus paraTypes reType ,
      ctx |- (TDeclFun idf params block reType) \in (funcTy paraTypes reType) ->
      ctx |- block \in reType ->
      ctx |- argus \in paraTypes ->
      ctx |- (TFun_call (TDeclFun idf params block reType) argus) \in reType

where "Gamma '|-' t '\in' T" := (has_type Gamma t T).



(* define operation semantics in small steps *)

Inductive value : tm -> Prop :=
  (* boolean true and false are values or normal form *)
  | v_true : 
       value BTrue
  | v_false : 
       value BFalse
  (* contants are values or normal form *)
  | v_const :
      forall n, value (ANum n)
(*
  | v_lnil : forall T,
      value (Tnil T)
  | v_lcons : forall vl vr,
      value vl ->
      value vr ->
      value (Tcons vl vr)
*)
  (* variable declaration is value *)
  | v_valDecl : forall idf T,
      value (TVal_decl T idf) 

  | v_para_lnil : 
      value (Para_nil)
  | v_argu_lnil : 
      value (Argu_nil)
  | v_para_list : forall vl vr,
      value vl ->
      value vr ->
      value (Para_list vl vr)

  | v_argu_list : forall vl vr,
      value vl ->
      value vr ->
      value (Argu_list vl vr)

  | v_none : 
      value CSkip
  (* function declaration is value*)
  | v_funDecl : 
      forall idf params block reType,
      value (TDeclFun idf params block reType).

Hint Constructors value.

(* operational semantics *)
Definition State := list (id * nat).

Definition empty_state : State := nil.
Fixpoint lookup (state : State) (V: id) : option nat := 
  match state with 
  | nil => None
  | (V', n') :: st => if beq_id V V' then Some n' else lookup st V
  end.

Fixpoint st_remove (st : State) (V:id) : State :=
  match st with
  | nil => nil
  | (V',n'):: st' => if beq_id V V' then st_remove st' V
                      else (V',n') :: st_remove st' V
  end.

Definition st_add (st: State) (V:id) (n:nat) : State :=
  (V,n) :: st_remove st V.


Reserved Notation "c1 '/' st '==>' c1' '/' st'"
  (at level 40, st at level 39, c1' at level 39).


Inductive step : tm -> State -> tm -> State -> Prop :=
  (* plus operation, evaluate the first operand first *)
  | ST_Plus1 : forall n1 n2 st,
       (APlus (ANum n1) (ANum n2)) / st ==> (ANum (plus n1 n2)) / st
  | ST_Plus2 : forall t1 st t1' st' t2,
       t1 / st ==> t1' / st' ->
       (APlus t1 t2) / st ==> (APlus t1' t2) / st'
  (* plus operation, if left operand is normal term, evaluate the second operand first *)
  | ST_Plus3 : forall n t2 st t2' st',
       t2 / st ==> t2' / st' ->
       (APlus (ANum n) t2) / st ==> (APlus (ANum n) t2') / st'


  (* minus operation, evaluate the first operand first *)
  | ST_Minus1 : forall n1 n2 st,
       (AMinus (ANum n1) (ANum n2)) / st ==> (ANum (minus n1 n2)) / st
  | ST_Minus2 : forall t1 st t1' st' t2,
       t1 / st ==> t1' / st' ->
       (AMinus t1 t2) / st ==> (AMinus t1' t2) / st'
  (* minus operation, if left operand is normal term, evaluate the second operand first *)
  | ST_Minus3 : forall n t2 st t2' st',
       t2 / st ==> t2' / st' ->
       (AMinus (ANum n) t2) / st ==> (AMinus (ANum n) t2') / st'


  (* mult operation, evaluate the first operand first *)
  | ST_Mult1 : forall n1 n2 st,
       (AMult (ANum n1) (ANum n2)) / st ==> (ANum (mult n1 n2)) / st
  | ST_Mult2 : forall t1 st t1' st' t2,
       t1 / st ==> t1' / st' ->
       (AMult t1 t2) / st ==> (AMult t1' t2) / st'
  (* minus operation, if left operand is normal term, evaluate the second operand first *)
  | ST_Mult3 : forall n t2 st t2' st',
       t2 / st ==> t2' / st' ->
       (AMult (ANum n) t2) / st ==> (AMult (ANum n) t2') / st'

  (* boolean equal *)
  | ST_BEq1 : forall t1 t1' t2 st st',
       t1 / st ==> t1' / st' ->
       BEq t1 t2 / st ==> BEq t1' t2 / st'
  | ST_BEq2 : forall n t2 t2' st st',
       t2 /st ==> t2' / st'  ->
       BEq (ANum n) t2 / st ==> BEq (ANum n) t2' /st'
  | ST_BEq3 : forall n1 n2 st, 
       BEq (ANum n1) (ANum n2) / st ==> (if beq_nat n1 n2 then BTrue else BFalse) / st

  (* boolean less than *)
  | ST_BLe1 : forall t1 t1' t2 st st',
       t1 / st ==> t1' / st' ->
       BLe t1 t2 / st ==> BLe t1' t2 / st'
  | ST_BLe2 : forall n t2 t2' st st',
       t2 / st ==> t2' / st' ->
       BLe (ANum n) t2 / st ==> BLe (ANum n) t2' / st'
  | ST_BLe3 : forall n1 n2 st,
       BLe (ANum n1) (ANum n2) / st ==> (if Nat.leb n1 n2 then BTrue else BFalse) / st

  (* boolean not *)
  | ST_BNot1 : forall t1 t1' st st',
       t1 / st  ==> t1' / st' ->
       BNot t1 / st ==> BNot t1' /st'
  | ST_BNot2 : forall st,
       BNot BTrue / st ==> BFalse / st
  | ST_BNot3 : forall st,
       BNot BFalse / st ==> BTrue / st

  (* boolean And *)
  | ST_BAnd1 : forall t1 t1' t2 st st',
       t1 / st ==> t1' / st'->
       BAnd t1 t2 / st  ==> BAnd t1' t2 / st'
  | ST_BAnd2 : forall st t1,
       BAnd BFalse t1 / st ==> BFalse / st
  | ST_BAnd3 : forall t st,
       BAnd BTrue t / st ==> t / st

  (* sequence *)
  | ST_Seq1 : forall t2 st,
      CSeq CSkip t2 / st ==> t2 / st
  | ST_Seq2 : forall t1 t1' t2 st st', 
      t1 / st ==> t1' / st' ->
      CSeq t1 t2 / st ==> CSeq t1' t2 / st'

  (* if statment *)
  | ST_IfTrue : forall t1 t2 st,
      (CIf BTrue t1 t2) / st ==> t1 / st
  | ST_IfFalse : forall t1 t2 st,
      (CIf BFalse t1 t2) / st ==> t2 / st
  | ST_If : forall t1 t1' t2 t3 st st',
      t1 / st ==> t1' / st' ->
      (CIf t1 t2 t3) / st ==> (CIf t1' t2 t3) / st'

  (* while command *)
  | ST_while_unfold : forall b t st,
      (CWhile b t) / st ==> CIf b (CSeq t (CWhile b t)) CSkip / st 

  (* assignment *)
  | ST_assign1 : forall x e e' st st',
      e / st ==> e' / st' ->
      CAss x e / st ==> CAss x e' / st'
  | ST_assign2 : forall x n st,
      CAss x (ANum n) / st ==> CSkip / (st_add st x n)


  (* veriable declaration *)
  | ST_Var_decl : forall idf ty st,
      TVal_decl ty idf / st ==> CSkip / (st_add st idf 0)

  (* list (to be finished) *)
  (*
  | ST_list1 : forall T st, 
      (Tnil T) / st ==> CSkip / st
  *)
  (* parameters *)
  | ST_para_list1 : forall t1 t1' t2 st st',
       t1 / st ==> t1' / st' ->
       (Para_list t1 t2) / st ==> (Para_list t1' t2) / st'
  | ST_para_list2 : forall v1 t2 t2' st st',
       value v1 ->
       t2 / st ==> t2' / st' -> 
       (Para_list v1 t2) / st ==> (Para_list v1 t2') / st'

  (* arguments *)
  | ST_argu_list1 : forall t1 t1' t2 st st',
       t1 / st ==> t1' / st' ->
       (Argu_list t1 t2) / st ==> (Argu_list t1' t2) / st'
  | ST_argu_list2 : forall v1 t2 t2' st st',
       value v1 ->
       t2 / st ==> t2' / st' -> 
       (Argu_list v1 t2) / st ==> (Argu_list v1 t2') / st'

  (* function call *)
(*  | ST_FunCall1 :
      forall st idf params block reType block' args st',
      block / st ==> block' / st' ->
      (TFun_call (TDeclFun idf params block reType) args) / st ==> (TFun_call (TDeclFun idf params block' reType) args) / st'
*)
  | ST_FunCall2 : 
      forall st idf params block reType args args' st',
      value (TDeclFun idf params block reType) ->
      args / st ==> args' / st' ->
      (TFun_call (TDeclFun idf params block reType) args) / st ==> (TFun_call (TDeclFun idf params block reType) args') / st'
  | ST_FunCall3 :
      forall idf params block reType args st,
      value args ->
      (TFun_call (TDeclFun idf params block reType) args) / st ==> (substArgus params args block) / st

  (* function return *)
  | ST_Return1 : forall e st e' st',
      e / st ==> e' / st' ->
      TFun_return e / st ==> TFun_return e' / st'
  | ST_Return2 : forall e st,
      value e ->
      TFun_return e / st ==> CSkip / st

where "c1 '/' st '==>' c1' '/' st'" := (step c1 st c1' st').

(* save for later use *)
(*
Definition relation (X: Type) := X->X->Prop.

Definition deterministic {X: Type} (R: relation X) :=
  forall x y1 y2 : X, R x y1 -> R x y2 -> y1 = y2.

Definition normal_form {X:Type} (R:relation X) (t:X) : Prop :=
  ~ exists t', R t t'.
*)


Definition step_normal_form (t : tm) (st : State) : Prop := ~ exists t' st', step t st t' st'. 

Definition stuck (t : tm) (st : State) : Prop := 
    step_normal_form t st /\ ~ value t.
Hint Unfold stuck.

(* some term is stuck *)
Example some_term_is_stuck :
  exists t st, stuck t st. 
Proof. 
  unfold stuck. (exists (APlus BTrue BFalse)). (exists nil).
  split.
    - unfold step_normal_form. intros contra. inversion contra. inversion H. inversion H0. inversion H6.
    - intros contra. inversion contra. 
Qed.


(* prove typing progress *)
(* 
   well typed normal forms are not stck or conversely, 
   if a term is well typed, then either it is value ofr it can take at least one step. 
   we call this progress
*)


(* 
  Canonical forms  
    the fundamental property that the definitions of boolean and numeric values
     agree with the typing relation.
*)
Lemma bool_canonical : forall t ctx, 
  ctx |- t \in termTy (mBool, rNone) -> value t -> t = BTrue \/ t = BFalse.
Proof. 
  intros. 
  inversion H0; auto. 
  induction H1. 
    - subst. inversion H. 
    - subst. inversion H. 
    - subst. inversion H. 
    - subst. inversion H. 
    - subst. inversion H.
    - subst. inversion H.
    - subst. inversion H.
Qed.


Lemma nat_canonical : forall t ctx,
 ctx |- t \in termTy (mNat, rNone) -> value t -> exists n, t = ANum n.
Proof.
  intros. inversion H0.
  - subst. inversion H.
  - subst. inversion H.
  - subst. exists n. reflexivity. 
  - subst. inversion H.
  - subst. inversion H.
  - subst. inversion H.
  - subst. inversion H.
  - subst. inversion H.
  - subst. inversion H.
Qed. 

(* Theorem: Suppose empty |- t : T.  Then either
       1. t is a value, or
       2. t ==> t' for some t'.
     Proof: By induction on the given typing derivation. 
*)

Lemma emptyCtxType : forall idf,
   tupleToPair (getTypeById emptyEnv idf) = (mNone, rNone).
Proof. 
    auto. 
Qed. 
 


Theorem progress : forall t T st, 
  emptyEnv |- t \in T -> value t \/ exists t' st', t / st ==> t' / st'.
Proof with auto. 
  intros t T st Ht.
  remember emptyEnv as Gamma.
  generalize dependent HeqGamma.
  induction Ht.
(*  induction Ht; intros HeqGamma; subst. *)
    (* T_Var *)
    (* The final rule in the given typing derivation cannot be 
       [T_Var], since it can never be the case that 
       [empty |- x : T] (since the context is empty). *)
    - right. subst.  rewrite emptyCtxType in H... contradiction.
    (* BTrue *)
    - left. auto.
    (* BFalse *)
    - left. auto. 
    (* Aplus *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* t2 is a value *)
          inversion H; subst; inversion Ht1.
          inversion H0; subst; inversion Ht2.
          exists (ANum (plus n n1)). exists st. apply ST_Plus1.
        * (* t2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          exists (APlus e1 t2'). exists st'.
          inversion H; subst; inversion Ht1.
          apply ST_Plus3. assumption.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (APlus t1' e2). exists st'.
          apply ST_Plus2. assumption.
    (* AMinus *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* t2 is a value *)
          inversion H; subst; inversion Ht1.
          inversion H0; subst; inversion Ht2.
          exists (ANum (minus n n1)). exists st. apply ST_Minus1.
        * (* t2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          exists (AMinus e1 t2'). exists st'.
          inversion H; subst; inversion Ht1.
          apply ST_Minus3. assumption.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (AMinus t1' e2). exists st'.
          apply ST_Minus2. assumption.
    (* AMult *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* t2 is a value *)
          inversion H; subst; inversion Ht1.
          inversion H0; subst; inversion Ht2.
          exists (ANum (mult n n1)). exists st. apply ST_Mult1.
        * (* t2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          exists (AMult e1 t2'). exists st'.
          inversion H; subst; inversion Ht1.
          apply ST_Mult3. assumption.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (AMult t1' e2). exists st'.
          apply ST_Mult2. assumption.
    (* ANum *)
    - left. apply v_const.
    (* BEq *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* t2 is a value *)
          inversion H; subst; inversion Ht1.
          inversion H0; subst; inversion Ht2.
          exists (if beq_nat n n1 then BTrue else BFalse). exists st. apply ST_BEq3.
        * (* t2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          exists (BEq e1 t2'). exists st'.
          inversion H; subst; inversion Ht1.
          apply ST_BEq2. assumption.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (BEq t1' e2). exists st'.
          apply ST_BEq1. assumption.
    (* BLe *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* t2 is a value *)
          inversion H; subst; inversion Ht1.
          inversion H0; subst; inversion Ht2.
          exists (if Nat.leb n n1 then BTrue else BFalse). exists st. apply ST_BLe3.
        * (* t2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          exists (BLe e1 t2'). exists st'.
          inversion H; subst; inversion Ht1.
          apply ST_BLe2. assumption.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (BLe t1' e2). exists st'.
          apply ST_BLe1. assumption.
    (* BNot *)
    - right. destruct IHHt...
      + (* e is a value *)
         inversion H; subst; inversion Ht.
         exists BFalse. exists st. apply ST_BNot2.
         exists BTrue. exists st. apply ST_BNot3.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (BNot t1'). exists st'.
          apply ST_BNot1. assumption.
    (* BAnd *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* t2 is a value *)
          inversion H; subst; inversion Ht1.
          exists e2. exists st. apply ST_BAnd3.
          exists BFalse. exists st. apply ST_BAnd2.
        * (* t2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          inversion H. 
          exists e2. exists st. apply ST_BAnd3.
          exists BFalse. exists st. apply ST_BAnd2.
          subst; inversion Ht1.
          subst; inversion Ht1.
          subst; inversion Ht1.
          subst; inversion Ht1.
          subst; inversion Ht1.
          subst; inversion Ht1.
          subst; inversion Ht1.
          subst; inversion Ht1.
      + (* e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (BAnd t1' e2). exists st'.
          apply ST_BAnd1. assumption.
      (* CSkip *)
    - left.
          apply v_none.
   (* CSeq *)
    - right. destruct IHHt1...
      + (* e1 is a value *)
        destruct IHHt2...
        * (* e1 is cskip *)
          inversion H; subst; inversion Ht1.
          exists (s). exists st. apply ST_Seq1.

        * (* e2 steps *)
          inversion H0 as [t2' Hstp].
          inversion Hstp as [st' HH].
          inversion H. subst. inversion Ht1.
          subst. inversion Ht1.
          subst. inversion Ht1.
          subst. inversion Ht1.
          subst. inversion Ht1.
          subst. inversion Ht1.
          subst. inversion Ht1.
          subst. inversion Ht1.
          exists (s). exists st. apply ST_Seq1.
          subst. inversion Ht1.
      + (* if e1 steps *)
          inversion H as [t1' Hstp].
          inversion Hstp as [st' HH].
          exists (CSeq t1' s). exists st'.
          apply ST_Seq2. assumption.

   (* CIf *)
    - right. destruct IHHt1...
        destruct IHHt2...
        * (* b is value *)
          inversion H; subst; inversion Ht1.
          exists (c1). exists st. apply ST_IfTrue.
          exists (c2). exists st. apply ST_IfFalse.
        * (* c1 steps *)
          inversion H; subst; inversion Ht1.
          exists (c1). exists st. apply ST_IfTrue.
          exists (c2). exists st. apply ST_IfFalse.
        * (* b steps *)
          inversion H as [t']. inversion H0 as [st'].
          exists (CIf t' c1 c2). exists st'. apply ST_If. assumption.


   (* CWhile *)
    - right. 
        exists (CIf e (CSeq c (CWhile e c)) CSkip). exists st. apply  ST_while_unfold.

   (* CAss *)
    - right. destruct IHHt1...
        destruct IHHt2...
        * (* e is value *)
          inversion H0; subst.
          inversion H. inversion H. exists CSkip. exists (st_add st x n). apply ST_assign2.
          inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. 
        * (* e steps *)
          inversion H0 as [t']. inversion H1 as [st']. 
          exists (CAss x t'). exists st'. apply ST_assign1. assumption.
        * (* x steps *)
          inversion H. inversion H0. inversion H1. 
     - right. destruct IHHt1...
        destruct IHHt2...
        * (* e is value *)
          inversion H0; subst.
          inversion H. inversion H. exists CSkip. exists (st_add st x n). apply ST_assign2.
          inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. inversion H.
        * (* e steps *)
          inversion H0 as [t']. inversion H1 as [st']. 
          exists (CAss x t'). exists st'. apply ST_assign1. assumption.
        * (* x steps *)
          inversion H. inversion H0. inversion H1. 
     - right. destruct IHHt1...
        destruct IHHt2...
        * (* e is value *)
          inversion H0; subst.
          inversion H. inversion H. exists CSkip. exists (st_add st x n). apply ST_assign2.
          inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. 
        * (* e steps *)
          inversion H0 as [t']. inversion H1 as [st']. 
          exists (CAss x t'). exists st'. apply ST_assign1. assumption.
        * (* x steps *)
          inversion H. inversion H0. inversion H1. 
     - right. destruct IHHt1...
        destruct IHHt2...
        * (* e is value *)
          inversion H0; subst.
          inversion H. inversion H. exists CSkip. exists (st_add st x n). apply ST_assign2.
          inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. inversion H. 
        * (* e steps *)
          inversion H0 as [t']. inversion H1 as [st']. 
          exists (CAss x t'). exists st'. apply ST_assign1. assumption.
        * (* x steps *)
          inversion H. inversion H0. inversion H1. 

    (* variable declaration *)
    - right. destruct IHHt...
          inversion H. inversion H. inversion H0. inversion H1.
    (* parameters *)
    - left. apply v_para_lnil.
    - destruct IHHt1...
        destruct IHHt2...

        inversion H0 as [t'].  inversion H1 as [st']. 
        right. exists (Para_list (TVal_decl decTy idf) t'). exists st'. apply ST_para_list2...
        inversion H as [t'].  inversion H0 as [st'].
        inversion H1. 

    (* Arguments *)
    - left. apply v_argu_lnil.
    - destruct IHHt1...
        destruct IHHt2...

        inversion H0 as [t'].  inversion H1 as [st']. 
        right. exists (Argu_list t1 t'). exists st'. apply ST_argu_list2...
        inversion H as [t'].  inversion H0 as [st'].
        right. exists (Argu_list t' t2). exists st'. apply ST_argu_list1...

    (* function declaration *)
    - left. apply v_funDecl.

    (* function return *)
    - destruct IHHt...
      + right. exists (CSkip). exists st. apply ST_Return2. assumption. 
      + right. inversion H as [t'].  inversion H0 as [st']. exists (TFun_return t'). exists st'. apply ST_Return1. assumption.

    (* function call *)
    - right.
     destruct IHHt3...
     exists (substArgus params argus block). exists st. apply ST_FunCall3. assumption.
     inversion H. inversion H0 as [st']. 
     exists (TFun_call (TDeclFun idf params block reType) x). exists st'.
     apply  ST_FunCall2...

Qed. 



(* substitution lemma *)
(** _Lemma_: If [Gamma,x:U |- t \in T] and [|- v \in U], then [Gamma |-
    [x:=v]t \in T]. *)

Lemma substitution_preserves_typing : forall Gamma x U t v T,
     update Gamma x U |- t \in T ->
     emptyEnv |- v \in (termTy U)   ->
     Gamma |- subst x v t \in T.
Proof.

Admitted. 

Definition getTypeInfo (t:ty) : typeInfo :=
  match t with
    | termTy ti => ti
    | _ => (mNone, rNone)
  end. 

Definition getFst (t : ty) : ty :=
  match t with 
    | prodTy t1 t2 => t1
    | _ => termTy (mNone, rNone)
  end.

Definition getSnd (t : ty) : ty :=
  match t with 
    | prodTy t1 t2 => t2
    | _ => termTy (mNone, rNone)
  end.


Inductive appears_free_in : id -> tm -> Prop :=
  | afi_var : forall x,
      appears_free_in x (Tvar x)
  | afi_app1 : forall x t1 t2,
      appears_free_in x t1 -> appears_free_in x (TFun_call t1 t2)
  | afi_app2 : forall x t1 t2,
      appears_free_in x t2 -> appears_free_in x (TFun_call t1 t2)
  | afi_para_nil : forall x,
      appears_free_in x (Para_nil)
  | afi_cons : forall x t2 decTy idf, 
      x <> idf ->
      appears_free_in x (t2) ->
      appears_free_in x (Para_list (TVal_decl decTy idf) t2)
  | afi_abs : forall x idf reType block params,
      appears_free_in x params ->
      appears_free_in x block ->
      appears_free_in x (TDeclFun idf params block reType)
  | afi_if1 : forall x t1 t2 t3,
      appears_free_in x t1 ->
      appears_free_in x (CIf t1 t2 t3)
  | afi_if2 : forall x t1 t2 t3,
      appears_free_in x t2 ->
      appears_free_in x (CIf t1 t2 t3)
  | afi_if3 : forall x t1 t2 t3,
      appears_free_in x t3 ->
      appears_free_in x (CIf t1 t2 t3).

Hint Constructors appears_free_in.

Lemma context_invariance : forall ctx ctx' t T,
     ctx |- t \in T  ->
     (forall x, appears_free_in x t -> 
     (getTypeById ctx x) = (getTypeById ctx' x))->
     ctx' |- t \in T.
Proof with eauto.
  intros.
  generalize dependent ctx'.
  inversion H; intros. subst. 
  - (* T_Var *)
    rewrite H4. apply TR_Var. rewrite H4 in H0. assumption.
    apply afi_var.     apply afi_var...
  - (* B_true *)
    apply TR_BTrue.
  - (* B_false *)
    apply TR_BFalse.
  - (* plus *)
    apply TR_Plus. auto. 
Admitted. 



Lemma subst_list_preserves_typing : forall params args fun_body T AT,
    emptyEnv |- fun_body \in T ->
    emptyEnv |- args \in AT ->
    emptyEnv |- substArgus params args fun_body \in T.
Proof.
  remember emptyEnv as Gamma.
  generalize dependent HeqGamma.
    intros. destruct params.
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. assumption. 
    - simpl. 
 destruct params1.
       auto. auto... auto. auto. 
       auto. auto... auto. auto. 
       auto. auto... auto. auto. 
       auto. auto... auto. auto. 
         destruct args.
          auto. auto... auto. auto. 
          auto. auto... auto. auto. 
          auto. auto... auto. auto. 
          auto. auto... auto. auto. 
          auto. auto... auto. simpl.
         assert (Gamma |- (subst i args1 fun_body) \in T).
         apply (substitution_preserves_typing Gamma i (getTypeInfo (getFst AT)) fun_body args1 T)...
         apply context_invariance with (ctx:=Gamma) (ctx':=(update Gamma i (getTypeInfo (getFst AT))))
              (t:=fun_body) (T:=T).
         assumption. intros...

Admitted. 


(* prove typing preservation*)

Theorem preservation : forall t t' T st st', 
    emptyEnv |- t \in T ->
    t / st ==> t' / st' ->
    emptyEnv |- t' \in T.

Proof with eauto. 
  remember emptyEnv as Gamma.
  intros t t' T st st' HT. generalize dependent t'.
  induction HT.
  - intros. inversion H0...
  - intros. inversion H. 
  - intros. inversion H.
  - intros. inversion H... subst. apply TR_Num.  apply TR_Plus. 
      subst. apply IHHT1... assumption.  
      subst. apply TR_Plus...
  - intros. inversion H... subst. apply TR_Num.  apply TR_Minus. 
      subst. apply IHHT1... assumption.  
      subst. apply TR_Minus...
  - intros. inversion H... subst. apply TR_Num.  apply TR_Mult. 
      subst. apply IHHT1... assumption.  
      subst. apply TR_Mult...
  - intros. inversion H.
  - intros. inversion H... subst. apply TR_BEq.
      apply IHHT1... assumption. subst. apply TR_BEq...
      subst. case_eq (n1 =? n2).
      intros. apply TR_BTrue.
      intros. apply TR_BFalse.
  - intros. inversion H... subst. apply TR_BLe.
      apply IHHT1... assumption. subst. apply TR_BLe...
      subst. case_eq (n1 <=? n2).
      intros. apply TR_BTrue.
      intros. apply TR_BFalse.
  - intros. inversion H.
      subst. apply TR_BNot. apply IHHT... 
      apply TR_BFalse.
      apply TR_BTrue.
  - intros. inversion H. 
      subst. apply TR_BAnd. apply IHHT1...  assumption.
      subst. apply TR_BFalse. rewrite H3 in HT2. assumption.
  - intros. inversion H.
  - intros. inversion H.
      rewrite H3 in HT2. assumption. subst. apply TR_CSeq. apply IHHT1 in H5. assumption... reflexivity. assumption. 
  - intros. inversion H. 
      subst. assumption. subst. assumption. subst. apply TR_CIfcondition. apply IHHT1... assumption. assumption. 
  - intros. inversion H. 
      subst. apply TR_CIfcondition. assumption.  apply TR_CSeq. assumption. 
      apply TR_CWhile. assumption. assumption. apply TR_CSkip.
  - intros. inversion H.
      subst. apply IHHT2 in H5. apply TR_CAss1 with (T:= T). 
      assumption. assumption. auto. apply TR_CSkip.
  - intros. inversion H. 
      subst. apply IHHT2 in H5. apply TR_CAss2 with (T:=T) (rt:=rt). 
      assumption. assumption. auto. apply TR_CSkip.
  - intros. inversion H. 
      subst. apply IHHT2 in H5. apply TR_CAss3 with (T:=T) (rt:=rt). 
      assumption. assumption. auto. apply TR_CSkip.
  - intros. inversion H0. 
      subst. apply IHHT2 in H6. apply TR_CAss4 with (T:=T) (rt1:=rt2) (rt2:=rt2). 
      assumption. assumption. reflexivity. auto.  apply TR_CSkip.
  - intros. inversion H. 
      apply TR_CSkip.
  - intros. inversion H.
  - intros. inversion H.
      subst.  apply IHHT1 in H5. apply TR_Cons. assumption. assumption.  
      subst...  apply IHHT2 in H6. apply TR_Cons. assumption. assumption. assumption.
  - intros. inversion H1.
  - intros. inversion H.
      apply IHHT in H1. apply TR_funreturn with (T:= T). assumption. auto. apply TR_CSkip. 
  - intros. inversion H.
      subst. apply TR_funcall1 with (paraTypes:=paraTypes).
      assumption.  assumption. apply IHHT3 in H9. assumption. 
      subst. auto. apply subst_list_preserves_typing. assumption.
Qed.

Inductive multiStep : tm -> State -> tm -> State -> Prop :=
  | multi_refl : forall t st,
      multiStep t st t st
  | multi_step : forall t1 t2 t3 st1 st2 st3,
      step t1 st1 t2 st2 ->
      multiStep t2 st2 t3 st3 ->
      multiStep t1 st1 t3 st3.

Corollary soundness : forall t t' T st st',
  emptyEnv |- t \in T ->
  multiStep t st t' st' ->
  ~ (stuck t' st').
Proof.
  intros t t' T st st' Hhas_type Hmulti. 
  intros [Hnf Hnot_val]. 
  induction Hmulti.
  destruct (progress t T st Hhas_type); auto.
  apply IHHmulti. apply (preservation t1 t2 T st1 st2 Hhas_type H). auto.
  auto. 
Qed.

