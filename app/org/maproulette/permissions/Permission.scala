package org.maproulette.permissions

import javax.inject.{Inject, Provider}

import com.google.inject.Singleton
import org.maproulette.actions._
import org.maproulette.exception.NotFoundException
import org.maproulette.models.dal.DALManager
import org.maproulette.models._
import org.maproulette.session.User

/**
  * @author cuthbertm
  */
@Singleton
class Permission @Inject() (dalManager: Provider[DALManager]) {
  /**
    * Checks to see if the object has read access to the object in request. Will throw an
    * IllegalAccessException if it does not have access. Read access is available on all objects
    * except User objects, users can only read their own user objects or all user objects if they are
    * a super user.
    *
    * @param obj The object you are checking to see if the user has read access on the object
    * @param user The user requesting the access.
    */
  def hasReadAccess(obj:BaseObject[Long], user:User) : Unit = {
    if (!user.isSuperUser) {
      obj.itemType match {
        case UserType() if obj.id != user.id =>
          throw new IllegalAccessException(s"User does not have read access to requested user object [${obj.id}]")
        case _ => // don't do anything, they have access
      }
    }
  }

  /**
    * Checks read access based purely on the id and item type, will throw a NotFoundException if
    * no object of the given type with the given id can be found. Delegates permission check to
    * object based hasReadAccess function
    *
    * @param itemType The type of item that user is checked read access against
    * @param user The user checking whether they have access or not
    * @param id The id of the object
    */
  def hasReadAccess(itemType:ItemType, user:User)(implicit id:Long) : Unit = {
    if (!user.isSuperUser) {
      itemType match {
        case UserType() if id != user.id =>
          throw new IllegalAccessException(s"User does not have read access to requested user object [$id]")
        case _ =>
          // Don't actually check, because currently all users will have read access on all objects
          /*dalManager.getManager(itemType).retrieveById match {
            case Some(obj) => hasReadAccess(obj.asInstanceOf[BaseObject[Long]], user)
            case None => throw new NotFoundException(s"No ${Actions.getTypeName(itemType.typeId).getOrElse("Unknown")} found using id [$id] to check read access")
          }*/
      }
    }
  }

  /**
    * Checks whether a user has write permission to the given object
    *
    * @param obj The object in question
    * @param user The user requesting write access
    */
  def hasWriteAccess(obj:BaseObject[Long], user:User) : Unit = {
    if (!user.isSuperUser) {
      obj.itemType match {
        case UserType() => hasReadAccess(obj, user)
        case ProjectType() => hasProjectAccess(Some(obj.asInstanceOf[Project]), user)
        case ChallengeType() =>
          hasProjectAccess(dalManager.get().challenge.retrieveRootObject(Right(obj.asInstanceOf[Challenge]), user), user)
        case SurveyType() =>
          hasProjectAccess(dalManager.get().survey.retrieveRootObject(Right(obj.asInstanceOf[Survey]), user), user)
        case TaskType() =>
          hasProjectAccess(dalManager.get().task.retrieveRootObject(Right(obj.asInstanceOf[Task]), user), user)
        case TagType() =>
        case GroupType() =>
          // Currently only super users have access to group objects
          throw new IllegalAccessException(s"Only super users have access to group objects")
      }
    }
  }

  /**
    * Checks if a user is a super user
    *
    * @param user
    */
  def hasSuperAccess(user:User) : Unit = if (!user.isSuperUser) {
    throw new IllegalAccessException(s"Only super users can perform this action.")
  }

  def hasWriteAccess(itemType:ItemType, user:User)(implicit id:Long) : Unit = {
    if (!user.isSuperUser) {
      itemType match {
        case UserType() => hasReadAccess(itemType, user)
        case _ =>
          dalManager.get().getManager(itemType).retrieveById match {
            case Some(obj) => hasWriteAccess(obj.asInstanceOf[BaseObject[Long]], user)
            case None => throw new NotFoundException(s"No ${Actions.getTypeName(itemType.typeId).getOrElse("Unknown")} found using id [$id] to check write access")
          }
      }
    }
  }

  private def hasProjectAccess(project:Option[Project], user:User) : Unit = {
    if (!user.isSuperUser) {
      project match {
        case Some(p) =>
          if (!user.groups.exists(p.id == _.id)) {
            throw new IllegalAccessException(s"User [${user.id}] does not have access to this project [${p.id}]")
          }
        case None =>
          throw new NotFoundException(s"No project found to check access for object")
      }
    }
  }
}
