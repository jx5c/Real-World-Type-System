(* a set of real-world types *)
Inductive rType: Set := rNone | degree | radians | climbSpeed | time | elevation.

Definition rTRule_mult (t1:rType) (t2:rType) : option rType := 
  match t1, t2 with 
    | climb_speed, time => Some elevation
    | _, _ => None
  end.


Definition rTRule_plus (t1:rType) (t2:rType) : option rType := 
  match t1, t2 with 
    | _, _ => None
  end.


Definition rTRule_minus (t1:rType) (t2:rType) : option rType := 
  match t1, t2 with 
    | _, _ => None
  end.

Definition rTRule_divide (t1:rType) (t2:rType) : option rType := 
  match t1, t2 with 
    | _, _ => None
  end.


Print rTRule_plus.