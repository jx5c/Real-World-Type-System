Require Import Coq.Arith.Arith.
Require Import Coq.Bool.Bool.
Require Import Coq.Strings.String.
Require Import Coq.Arith.EqNat.
Require Import Coq.omega.Omega.
Require Import Coq.Lists.List.


Require Import rwt_example.

(* a set of machine types *)
Inductive mType: Set := mBool | mNum.

(* type for identifiers *)
Inductive id : Type :=
  | Id : string -> id.

(* comparison of identifiers *)
Definition beq_id x y :=
  match x,y with
    | Id n1, Id n2 => if string_dec n1 n2 then true else false
  end.

Definition typeInfo : Type := mType * rType.

Inductive ty : Type :=
  | TermTy : typeInfo -> ty
  | TArrow : ty -> ty -> ty.


(* terms *)
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
(*
  | BEq : tm -> tm -> tm
  | BLe : tm -> tm -> tm
  | BNot : tm -> tm
  | BAnd : tm -> tm -> tm
*)
  (* commands *)
  | CSkip : tm
  | CAss : id -> tm -> tm
  | CSeq : tm -> tm -> tm
  | CIf : tm -> tm -> tm -> tm
  | CWhile : tm -> tm -> tm

  (* function declaration *)
  | TAbs : id -> ty -> tm -> tm
  (* function call*)
  | TApp : tm -> tm -> tm.

(*
Inductive tm : Type :=
  | tvar : id -> tm
  | tapp : tm -> tm -> tm
  | tabs : id -> ty -> tm -> tm
  | ttrue : tm
  | tfalse : tm
  (* arithematic *)
  | ANum : nat -> tm 
  | tmult : tm -> tm -> tm
  | tif : tm -> tm -> tm -> tm.
*)

Inductive value : tm -> Prop :=
  | v_abs : forall x T t,
      value (TAbs x T t)
  | v_true :
      value BTrue
  | v_false :
      value BFalse.

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
(*
  | BEq t1 t2 =>
     BEq (subst x s t1) (subst x s t2)
  | BLe t1 t2 =>
     BLe (subst x s t1) (subst x s t2)
  | BNot t =>
     BNot (subst x s t)
  | BAnd t1 t2 =>
     BAnd (subst x s t1) (subst x s t2)
*)
  | CSkip => CSkip
  | CAss idf t =>
     CAss idf (subst x s t)
  | CSeq t1 t2 =>
     CSeq (subst x s t1) (subst x s t2)
  | CIf t1 t2 t3 =>
     CIf (subst x s t1) (subst x s t2) (subst x s t3)
  | CWhile t1 t2 =>
     CWhile (subst x s t1) (subst x s t2)
  | TAbs x' T t1 =>
      TAbs x' T (if beq_id x x' then t1 else ([x:=s] t1))
  | TApp t1 t2 =>
      TApp (subst x s t1) (subst x s t2)
  end
where "'[' x ':=' s ']' t" := (subst x s t).


Reserved Notation "t1 '==>' t2" (at level 40).

Inductive step : tm -> tm -> Prop :=
  | ST_AppAbs : forall x T t12 v2,
         value v2 ->
         (TApp (TAbs x T t12) v2) ==> [x:=v2]t12
  | ST_App1 : forall t1 t1' t2,
         t1 ==> t1' ->
         TApp t1 t2 ==> TApp t1' t2
  | ST_App2 : forall v1 t2 t2',
         value v1 ->
         t2 ==> t2' ->
         TApp v1 t2 ==> TApp v1  t2'
  | ST_IfTrue : forall t1 t2,
      (CIf BTrue t1 t2) ==> t1
  | ST_IfFalse : forall t1 t2,
      (CIf BFalse t1 t2) ==> t2
  | ST_If : forall t1 t1' t2 t3,
      t1 ==> t1' ->
      (CIf t1 t2 t3) ==> (CIf t1' t2 t3)

where "t1 '==>' t2" := (step t1 t2).

Hint Constructors step.
Reserved Notation "Gamma '|-' t '\in' T" (at level 40).





Definition total_map (A:Type) := id -> A.
Definition partial_map (A:Type) := total_map (option A).
Definition context := partial_map ty.

Definition t_update {A:Type} (m : total_map A)
                    (x : id) (v : A) :=
  fun x' => if beq_id x x' then v else m x'.

Definition update {A:Type} (m : partial_map A)
                  (x : id) (v : A) :=
  t_update m x (Some v).

Definition t_empty {A:Type} (v : A) : total_map A :=
  (fun _ => v).


Reserved Notation "Gamma '|-' t '\in' T" (at level 40).

(*
Inductive has_type : context -> tm -> ty -> Prop :=
  | T_Var : forall Gamma x T,
      Gamma x = Some T ->
      Gamma |- tvar x \in T
  | T_Abs : forall Gamma x T11 T12 t12,
      update Gamma x T11 |- t12 \in T12 ->
      Gamma |- tabs x T11 t12 \in TArrow T11 T12
  | T_App : forall T11 T12 Gamma t1 t2,
      Gamma |- t1 \in TArrow T11 T12 ->
      Gamma |- t2 \in T11 ->
      Gamma |- tapp t1 t2 \in T12
  | T_True : forall Gamma,
       Gamma |- ttrue \in (TermTy (mBool, rNone))
  | T_False : forall Gamma,
       Gamma |- tfalse \in (TermTy (mBool, rNone))
  | T_If : forall t1 t2 t3 T Gamma,
       Gamma |- t1 \in (TermTy (mBool, rNone)) ->
       Gamma |- t2 \in T ->
       Gamma |- t3 \in T ->
       Gamma |- tif t1 t2 t3 \in T

where "Gamma '|-' t '\in' T" := (has_type Gamma t T).

Hint Constructors has_type.
*)

(* type checking part *)
Fixpoint beq_ty (T1 T2:ty) : bool :=
  match T1,T2 with
  | (TermTy (mBool, _)) , (TermTy (mBool, _)) =>
      true
  | (TermTy (mNum, _)) , (TermTy (mNum, _)) =>
      true
  | TArrow T11 T12, TArrow T21 T22 =>
      andb (beq_ty T11 T21) (beq_ty T12 T22)
  | _,_ =>
      false
  end.


Module STLCChecker.

Import rwt_example.
Print rType.
Print rTRule_mult.
Print rTRule_plus. 

Fixpoint type_check (Gamma:context) (t:tm) : option ty :=
  match t with
  | Tvar x =>
      Gamma x
  | ANum n =>
      Some (TermTy (mNum, rNone))
  | APlus t1 t2 =>
      match type_check Gamma t1, type_check Gamma t2 with
      | Some (TermTy (mNum, _)), Some (TermTy (mNum, rNone)) => Some (TermTy (mNum, rNone))
      | Some (TermTy (mNum, rNone)), Some (TermTy (mNum, _)) => Some (TermTy (mNum, rNone))
      | Some (TermTy (mNum, rwt1)), Some (TermTy (mNum, rwt2)) => 
          match (rTRule_plus rwt1 rwt2) with 
            | Some rwt_r => Some (TermTy (mNum, rwt_r))
            | None => None
          end
      | _,_ => None
      end
  | AMinus t1 t2 =>
      match type_check Gamma t1, type_check Gamma t2 with
      | Some (TermTy (mNum, _)), Some (TermTy (mNum, rNone)) => Some (TermTy (mNum, rNone))
      | Some (TermTy (mNum, rNone)), Some (TermTy (mNum, _)) => Some (TermTy (mNum, rNone))
      | Some (TermTy (mNum, rwt1)), Some (TermTy (mNum, rwt2)) => 
          match (rTRule_minus rwt1 rwt2) with 
            | Some rwt_r => Some (TermTy (mNum, rwt_r))
            | None => None
          end
      | _,_ => None
      end

   | AMult t1 t2 =>
      match type_check Gamma t1, type_check Gamma t2 with
      | Some (TermTy (mNum, _)), Some (TermTy (mNum, rNone)) => Some (TermTy (mNum, rNone))
      | Some (TermTy (mNum, rNone)), Some (TermTy (mNum, _)) => Some (TermTy (mNum, rNone))
      | Some (TermTy (mNum, rwt1)), Some (TermTy (mNum, rwt2)) => 
          match (rTRule_mult rwt1 rwt2) with 
            | Some rwt_r => Some (TermTy (mNum, rwt_r))
            | None => None
          end
      | _,_ => None
      end
  | BTrue =>
      Some (TermTy (mBool, rNone))
  | BFalse =>
      Some (TermTy (mBool, rNone))

  | TAbs x T11 t12 =>
      match type_check (update Gamma x T11) t12 with
      | Some T12 => Some (TArrow T11 T12)
      | _ => None
      end
  | TApp t1 t2 =>
      match type_check Gamma t1, type_check Gamma t2 with
      | Some (TArrow T11 T12),Some T2 =>
          if beq_ty T11 T2 then Some T12 else None
      | _,_ => None
      end

  | CIf guard t f =>
      match type_check Gamma guard with
      | Some (TermTy (mBool, rNone)) =>
          match type_check Gamma t, type_check Gamma f with
          | Some T1, Some T2 =>
              if beq_ty T1 T2 then Some T1 else None
          | _,_ => None
          end
      | _ => None
      end

  | CAss idf t => 
      let tl:=(Gamma idf) in 
      match type_check Gamma t with
          | tl => Gamma idf
      end

  | CSeq t1 t2 =>
      match type_check Gamma t1, type_check Gamma t2 with
          | Some T1, Some T2 => Some T2
          | _,_ => None
      end

  | CWhile guard t =>
      match type_check Gamma guard with
      | Some (TermTy (mBool, rNone)) => 
          match type_check Gamma t with
          | Some T1 => Some T1
          | _ => None
          end
      | _ => None
      end
  | _ => None 
  end.


Lemma beq_ty_refl : forall T1,
  beq_ty T1 T1 = true.
Proof.
Admitted. 

Lemma beq_ty__eq : forall T1 T2,
  beq_ty T1 T2 = true -> T1 = T2.
Proof with auto.
Admitted.

(*
Theorem type_checking_complete : forall Gamma t T,
  has_type Gamma t T -> type_check Gamma t = Some T.
Proof with auto.
Admitted. 


Theorem type_checking_sound : forall Gamma t T,
  type_check Gamma t = Some T -> has_type Gamma t T.
Proof with eauto.
Admitted.
*)

End STLCChecker.

Import rwt_example.

Definition ida := (Id "a").
Definition idb := (Id "b").
Definition aex1 : tm := AMult (Tvar ida) (Tvar idb).

Definition empty_env : context := t_empty None.

Print rType. 
Definition cls_ty := (TermTy (mNum, climbSpeed)).
Definition time_ty := (TermTy (mNum, time)).

Definition Gamma := update (update empty_env ida cls_ty) idb time_ty.

Compute Gamma ida.
Compute STLCChecker.type_check Gamma (Tvar ida).
Compute STLCChecker.type_check Gamma aex1.












