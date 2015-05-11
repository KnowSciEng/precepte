package com.mfglabs.monitoring

import scala.language.higherKinds

/**
 * A typeclass that is used to help guide scala's type inference to
 * find typeclass instances for types which have shapes which differ
 * from what their typeclasses are looking for.
 *
 * For example, [[Functor]] is defined for types in the shape
 * F[_]. Scala has no problem finding instance of Functor which match
 * this shape, such as Functor[Option], Functor[List], etc. There is
 * also a functor defined for some types which have the Shape F[_,_]
 * when one of the two 'holes' is fixed. For example. there is a
 * Functor for Map[A,?] for any A, and for Either[A,?] for any A,
 * however the scala compiler will not find them without some coercing.
 */
trait Unapply[TC[_[_]], MA] {
  // a type constructor which is properly kinded for the typeclass
  type M[_]
  // the type applied to the type constructor to make an MA
  type A

  // the actual typeclass instance found
  def TC: TC[M]

  // a function which will coerce the MA value into one of type M[A]
  // this will end up being the identity function, but we can't supply
  // it until we have proven that MA and M[A] are the same type
  def subst: MA => M[A]
}

object Unapply {
  // a convenience method for summoning Unapply instances
  def apply[TC[_[_]], MA](implicit ev: Unapply[TC,MA]): Unapply[TC, MA] = implicitly

  // the type we will instantiate when we find a typeclass instance
  // which is already the expected shape: F[_]
  type Aux1[TC[_[_]], MA, F[_], AA] = Unapply[TC, MA] {
    type M[X] = F[X]
    type A = AA
  }

  implicit def unapply1[TC[_[_]], F[_], AA](implicit tc: TC[F])
      : Aux1[TC,F[AA],F,AA] =
    new Unapply[TC,F[AA]] {
      type M[X] = F[X]
      type A = AA
      override def TC: TC[F] = tc
      override def subst = identity
  }

  // the type we will instantiate when we find a typeclass instance
  // for a type in the shape F[_,_] when we fix the left type
  type Aux2Left[TC[_[_]], FA, F[_,_], AA, B] = Unapply[TC, FA] {
    type M[X] = F[X,B]
    type A = AA
  }

  // the type we will instantiate when we find a typeclass instance
  // for a type in the shape F[_,_] when we fix the right type
  type Aux2Right[TC[_[_]], MA, F[_,_], AA, B] = Unapply[TC, MA] {
    type M[X] = F[AA,X]
    type A = B
  }


  implicit def unapply2left[TC[_[_]], F[_,_], AA, B](implicit tc: TC[({ type λ[α] = F[α, B] })#λ]): Aux2Left[TC,F[AA,B], F, AA, B] = new Unapply[TC, F[AA,B]] {
     type M[X] = F[X, B]
     type A = AA
     def TC: TC[({ type λ[α] = F[α, B] })#λ] = tc
     def subst = identity
   }

   implicit def unapply2right[TC[_[_]], F[_,_], AA, B](implicit tc: TC[({ type λ[α] = F[AA, α] })#λ]): Aux2Right[TC,F[AA,B], F, AA, B] = new Unapply[TC, F[AA,B]] {
     type M[X] = F[AA, X]
     type A = B
     def TC: TC[({ type λ[α] = F[AA, α] })#λ] = tc
     def subst = identity
   }

  // STEW: I'm not sure why these Nothing cases are needed and aren't
  // just caught by the generic cases, I'd love for someone to figure
  // that out and report back.
  implicit def unapply2leftN[TC[_[_]], F[_,+_], AA](implicit tc: TC[({ type λ[α] = F[α, Nothing] })#λ]): Aux2Left[TC,F[AA,Nothing], F, AA, Nothing] = new Unapply[TC, F[AA,Nothing]] {
     type M[X] = F[X, Nothing]
     type A = AA
     def TC: TC[({ type λ[α] = F[α, Nothing] })#λ] = tc
     def subst = identity
   }

  implicit def unapply2rightN[TC[_[_]], F[+_,_], B](implicit tc: TC[({ type λ[α] = F[Nothing, α] })#λ]): Aux2Right[TC,F[Nothing,B], F, Nothing, B] = new Unapply[TC, F[Nothing,B]] {
     type M[X] = F[Nothing, X]
     type A = B
     def TC: TC[({ type λ[α] = F[Nothing, α] })#λ] = tc
     def subst = identity
   }

  // the type we will instantiate when we find a typeclass instance
  // for a type in the shape of a Monad Transformer with 2 type params
  type Aux2MT[TC[_[_]], MA, F[_[_],_], AA[_], B] = Unapply[TC, MA] {
    type M[X] = F[AA,X]
    type A = B
  }

  implicit def unapply2Aux2MT[TC[_[_]], F[_[_],_], AA[_], B](implicit tc: TC[({ type λ[α] = F[AA, α] })#λ]):
    Aux2MT[TC, F[AA, B], F, AA, B] = new Unapply[TC, F[AA, B]] {
     type M[X] = F[AA, X]
     type A = B
     def TC: TC[({ type λ[α] = F[AA, α] })#λ] = tc
     def subst = identity
   }

  // the type we will instantiate when we find a typeclass instance
  // for a type in the shape of a Monad Transformer with 3 type params
  // F[_[_],_,_] when we fix the middle type
  type Aux3MTLeft[TC[_[_]], MA, F[_[_],_,_], AA[_], B, C] = Unapply[TC, MA] {
    type M[X] = F[AA,X,C]
    type A = B
  }

  // the type we will instantiate when we find a typeclass instance
  // for a type in the shape of a Monad Transformer with 3 type params
  // F[_[_],_,_] when we fix the right type
  type Aux3MTRight[TC[_[_]], MA, F[_[_],_,_], AA[_], B, C] = Unapply[TC, MA] {
    type M[X] = F[AA,B,X]
    type A = C
  }


  implicit def unapply3MTLeft[TC[_[_]], F[_[_],_,_], AA[_], B, C](implicit tc: TC[({ type λ[α] = F[AA, α, C] })#λ]): Aux3MTLeft[TC,F[AA, B, C], F, AA, B, C] = new Unapply[TC, F[AA,B,C]] {
     type M[X] = F[AA, X, C]
     type A = B
     def TC: TC[({ type λ[α] = F[AA, α, C] })#λ] = tc
     def subst = identity
   }

  implicit def unapply3MTright[TC[_[_]], F[_[_],_,_], AA[_], B, C](implicit tc: TC[({ type λ[α] = F[AA, B, α] })#λ]): Aux3MTRight[TC,F[AA,B,C], F, AA, B, C] = new Unapply[TC, F[AA,B,C]] {
     type M[X] = F[AA, B, X]
     type A = C
     def TC: TC[({ type λ[α] = F[AA, B, α] })#λ] = tc
     def subst = identity
   }

  // the type we will instantiate when we find a typeclass instance
  // for a type in the shape of a Monad Transformer with 2 type params
  type Aux21MT[TC[_[_]], MA, F[_[_],_], AA[_, _], A0, B] = Unapply[TC, MA] {
    type M[X] = F[({ type λ[α] = AA[A0, α] })#λ, X]
    type A = B
  }

  implicit def unapply2Aux21MT[TC[_[_]], F[_[_],_], AA[_, _], A0, B](
    implicit tc1: TC[({ type λ[ß] = AA[A0, ß] })#λ],
             // tc2: TC[({ type λ[ß] = F[scalaz.Id.Id, ß] })#λ],
             tc: TC[({ type λ[α] = F[({ type λ[ß] = AA[A0, ß] })#λ, α] })#λ]
  ): Aux21MT[TC, F[({ type λ[α] = AA[A0, α] })#λ, B], F, AA, A0, B] =
      new Unapply[TC, F[({ type λ[α] = AA[A0, α] })#λ, B]] {
        type M[X] = F[({ type λ[α] = AA[A0, α] })#λ, X]
        type A = B
        def TC: TC[({ type λ[α] = F[({ type λ[ß] = AA[A0, ß] })#λ, α] })#λ] = tc
        def subst = identity
      }
}
