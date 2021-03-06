package org.maproulette.exception

/**
  * Simple exception class extending exception, to handle invalid API calls. This allows us to pattern
  * match on the exception and if InvalidException is found we return a BadRequest instead of
  * an InternalServerError
  *
  * @param message The message to send with the exception
  */
class InvalidException(message:String) extends Exception(message)

/**
  * NotFoundException should be throw whenever we try to retrieve an object based on the object id
  * and find nothing
  *
  * @param message The message to send with the exception
  */
class NotFoundException(message:String) extends Exception(message)

/**
  * Exception for handling any exceptions related to locking of Map Roulette objects
  *
  * @param message The message to send with the exception
  */
class LockedException(message:String) extends Exception(message)

/**
  * Exception for handling the unique violations when trying to insert objects into the database
  *
  * @param message The message to send with the exception
  */
class UniqueViolationException(message:String) extends Exception(message)
