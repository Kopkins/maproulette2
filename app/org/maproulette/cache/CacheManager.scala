package org.maproulette.cache

import org.maproulette.models.BaseObject

import scala.collection.mutable

/**
  * A caching manager that can be used to wrap around blocks of code and that can cache the elements
  * that are retrieved by the inner block or return the cached item instead of executing the inner
  * block of code. The underlying cache is handled by the "CacheStorage" object, which is simply
  * just an in memory map. However CacheManagers should be overridden by whatever implementation
  * you require. In the future the CacheStorage should be defined by the configuration, but for
  * now a developer will have to manually override it.
  *
  * @author cuthbertm
  */
class CacheManager[Key, A<:BaseObject[Key]](cacheLimit:Int=10000, cacheExpiry:Int=900) {
  val cache = new CacheStorage[Key, A](cacheLimit, cacheExpiry)
  val nameCache = mutable.Map[String, Key]()

  def clearCaches = {
    cache.clear()
    nameCache.clear()
  }

  /**
    * Update the name cache to map to the id of the object with that name, and then makes sure that
    * the cacheManager contains that cached item as well
    *
    * @param name The name of the object you are looking for
    * @return An optional key for the object, None if not found
    */
  def updateNameCache(block:String => Option[A])(implicit name:String, caching:Boolean=true) : Option[Key] = {
    if (caching) {
      if (nameCache.contains(name)) {
        nameCache.get(name)
      } else {
        // check the cache
        cache.find(name) match {
          case Some(obj) => Some(obj.id)
          case None => block(name) match {
            case Some(obj) =>
              cache.add(obj)
              nameCache.put(name, obj.id)
              Some(obj.id)
            case None => None
          }
        }
      }
    } else {
      block(name) match {
        case Some(value) => Some(value.id)
        case None => None
      }
    }
  }


  /**
    * Helper function for withOptionCaching, the only difference is that it takes a non optional
    * id key
    *
    * @param block the function to be executed that would retrieve the object from primary storage
    * @param id The id of the object
    * @param caching default true, if false will skip checking the cache
    * @return An optional base object that is cached
    */
  def withCaching(block:() => Option[A])(implicit id:Key, caching:Boolean=true) : Option[A] =
    withOptionCaching(block)(Some(id), caching)

  /**
    * Hits the cache first to see if the object exists, if it doesn't then it will hit the
    * inner block of code and presumably retrieve the object and then place it into the cache.
    * If the object is found it will simply return it and not execute the inner block of code.
    *
    * @param id The id of the object, if None then will not hit the cache first
    * @param block the function to be executed that would retrieve the object from primary storage
    * @param caching default true, if false will skip checking the cache
    * @return An optional base object that is cached
    */
  def withOptionCaching(block:() => Option[A])
                       (implicit id:Option[Key]=None, caching:Boolean=true): Option[A] = {
    caching match {
      case true =>
        val cached = id match {
          case Some(key) => cache.get(key)
          case None => None
        }

        cached match {
          case Some(hit) => Some(hit)
          case None =>
            block() match {
              case Some(result) =>
                cache.add(result)
                Some(result)
              case None => None
            }
        }
      case false => block()
    }
  }

  /**
    * The updating cache will hit the cache first to see if the object exists in cache, and if it
    * does it will pass the object to the inner block of code. The inner block of code would then
    * update/retrieve the object from primary storage based on what is currently in the cache.
    * This is useful primarily for update methods, where you want to update only specific elements
    * of the object, but to make the code generic enough you need information on the other elements.
    * Or alternatively you could make decisions on the update based on the current state of the object.
    *
    * @param retrieve The retrieval function for the object, if it is not in the cache it would use
    *                 this function to retrieve it first from primary storage
    * @param block The inner block of code that would presumably be updating the object based on the
    *              current state of the object
    * @param id The id or key of the object that you will be updating
    * @param caching If false, then caching will be completely ignored and everything will execute
    *                against the primary storage
    * @return An optional base object that has been updated and cached.
    */
  def withUpdatingCache(retrieve:Key => Option[A])(block:A => Option[A])
                       (implicit id:Key, caching:Boolean=true, delete:Boolean=false) : Option[A] = {
    val cachedItem = caching match {
      case true =>
        cache.get(id) match {
          case Some(hit) => Some(hit)
          case None => retrieve(id) match {
            case Some(value) =>
              Some(value)
            case None => None
          }
        }
      case false => retrieve(id)
    }

    cachedItem match {
      case Some(item) =>
        block(item) match {
          case Some(updatedItem) =>
            if (caching) {
              if (delete) {
                cache.remove(updatedItem.id)
              } else {
                cache.add(updatedItem)
              }
            }
            Some(updatedItem)
          case None => None
        }
      case None => None
    }
  }

  /**
    * Will retrieve the object from the cache, and delete it afterwards
    *
    * @param retrieve The function to retrieve from external storage if not in cache
    * @param block The block of code to execute
    * @param id The id of the object
    * @param caching Whether caching is enabled or not
    * @return
    */
  def withDeletingCache(retrieve:Key => Option[A])(block:A => Option[A])
                     (implicit id:Key, caching:Boolean=true) : Option[A] = withUpdatingCache(retrieve)(block)(id, caching, true)

  /**
    * This will pull the items out of the cache that have been cached, and send the rest of the id's
    * to the database to retrieve the objects and place them in the cache after they have been retrieved
    *
    * @param block the lambda function that will be used to retrieve the objects
    * @param ids The ids of the objects to retrieve
    * @param caching If false, then caching will be completely ignored
    * @return A list of objects
    */
  def withIDListCaching(block:List[Key] => List[A])
                       (implicit ids:List[Key]=List(), caching:Boolean=true) : List[A] = {
    caching match {
      case true =>
        val connected = ids.map(id => (id, cache.get(id)))
        val unCachedIDs = connected.filter(value => value._2.isEmpty).map(_._1)
        // we execute the block if there are uncachedID's or if the original ids passed in is empty
        val cachedItems = if (unCachedIDs.nonEmpty || ids.isEmpty) {
          val uncachedList = block(unCachedIDs)
          (unCachedIDs zip uncachedList).foreach(obj => cache.add(obj._2))
          uncachedList ++ connected.flatMap(_._2)
        } else {
          connected.flatMap(_._2)
        }
        cachedItems.filter(entry => ids.isEmpty || unCachedIDs.contains(entry.id)).foreach(cache.add(_))
        cachedItems
      case false => block(ids)
    }
  }

  /**
    * This will remove the list of id's from the cache and then execute the inner block of code
    *
    * @param block The inner block of code to execute after the objects have been evicted from the cache
    * @param ids The ids of the objects that you wanted evicted from the cache
    * @param caching If caching is turned off, then it would simply execute the inner block of code
    * @tparam B This type of return object
    * @return The result of the inner block of code
    */
  def withCacheIDDeletion[B](block:() => B)(implicit ids:List[Key], caching:Boolean=true) : B = {
    if (caching) ids.foreach(cache.remove(_))
    block()
  }

  /**
    * Gets an object by name from the cache
    *
    * @param name The name of the object
    * @return An optional object found in the cache, None if not found
    */
  def getByName(name:String) : Option[A] = cache.find(name)

  /**
    * Evicts an object by name from the cache
    *
    * @param name The name of the object to be evicted
    * @return An optional object that was found in the cache to be evicted. None if not found and
    *         subsequently not deleted.
    */
  def deleteByName(name:String) : Option[A] = cache.find(name) match {
    case Some(obj) => cache.remove(obj.id)
    case None => None
  }

  /**
    * Checks caching based on a list of supplied names. If the name is in the cache it will retrieve
    * the object of the cache. It will supply the list of names to the inner block, which would then
    * presumably retrieve the objects based on the names supplied. Once those objects have been
    * retrieved it will join them up with the objects that were originally retrieved from the cache.
    *
    * @param block The inner block of code to execute, presumably to retrieve unfound objects
    * @param names The names of objects that you are looking for.
    * @param caching Whether caching is enabled or not, default is enabled.
    * @return A list of objects returned from cache and primary storage
    */
  def withNameListCaching(block:List[String] => List[A])
                         (implicit names:List[String]=List(), caching:Boolean=true) : List[A] = {
    caching match {
      case true =>
        val connected = names.map(name => (name, cache.find(name)))
        val unCachedIDs = connected.filter(value => value._2.isEmpty).map(_._1)
        if (unCachedIDs.nonEmpty) {
          val list = block(unCachedIDs)
          list.foreach(obj => cache.add(obj))
          list ++ connected.flatMap(_._2)
        } else {
          connected.flatMap(_._2)
        }
      case false => block(names)
    }
  }

  /**
    * Deletes the objects from the cache based on the list of names provided, prior to executing the
    * inner code block.
    *
    * @param block The inner block of code
    * @param names The list of names
    * @param caching Whether caching is enabled or not, default is enabled
    * @tparam B The type of return object from the inner block
    * @return The return object from the inner block
    */
  def withCacheNameDeletion[B](block:() => B)
                              (implicit names:List[String], caching:Boolean=true) : B = {
    if (caching) names.foreach(cache.remove(_))
    block()
  }

  /**
    * This will attempt to retrieve the item, and if it cannot it will create it and dump it in the cache
    * then return the item
    *
    * @param retrieve The function that is used to the retrieve the item if it is not in the cache
    * @param create The function to create the item if it is not found
    * @param item The item to find/create
    * @param caching Whether caching is turned on or off
    * @return The item that was potentially created and then retrieved from cache
    */
  def withCreatingCache(retrieve:Key => Option[A], create:A => A)(implicit item:A, caching:Boolean=true) : A = {
    implicit val id = item.id
    withUpdatingCache(retrieve) { implicit cached =>
      Some(cached)
    } match {
      case Some(a) => a
      case None =>
        val created = create(item)
        if (caching) cache.add(created)
        created
    }
  }
}
