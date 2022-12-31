package com.chrisgoldammer.cocktails.helpers

def toLongSafe(s: String): Option[Long] = {
  try {
    Some(s.toLong)
  } catch {
    case e: Exception => None
  }
}