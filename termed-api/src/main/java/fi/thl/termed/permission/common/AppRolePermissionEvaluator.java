package fi.thl.termed.permission.common;

import com.google.common.collect.Multimap;

import fi.thl.termed.domain.AppRole;
import fi.thl.termed.domain.Permission;
import fi.thl.termed.domain.User;
import fi.thl.termed.permission.PermissionEvaluator;

/**
 * Evaluates permission purely by users app role, i.e. actual object is ignored.
 */
public class AppRolePermissionEvaluator<E> implements PermissionEvaluator<E> {

  private Multimap<AppRole, Permission> rolePermissions;

  public AppRolePermissionEvaluator(Multimap<AppRole, Permission> rolePermissions) {
    this.rolePermissions = rolePermissions;
  }

  @Override
  public boolean hasPermission(User user, E object, Permission permission) {
    return rolePermissions.get(user.getAppRole()).contains(permission);
  }

}
