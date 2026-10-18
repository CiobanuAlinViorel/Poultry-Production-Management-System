import { describe, expect, it } from 'vitest';
import { canRead, canWrite, isSystemAdmin, ModuleRoles, Roles, userFromToken } from './access';

const token = (claims: object) => `h.${btoa(JSON.stringify(claims)).replace(/=+$/, '')}.s`;

describe('access', () => {
  it('SYSTEM_ADMIN can read and write every module', () => {
    const roles = [Roles.SYSTEM_ADMIN];
    expect(isSystemAdmin(roles)).toBe(true);
    for (const moduleRoles of Object.values(ModuleRoles)) {
      expect(canRead(roles, moduleRoles)).toBe(true);
      expect(canWrite(roles, moduleRoles)).toBe(true);
    }
  });

  it('MANAGER is read-only everywhere', () => {
    const roles = [Roles.MANAGER];
    expect(canRead(roles, ModuleRoles.farm)).toBe(true);
    expect(canRead(roles, ModuleRoles.admin)).toBe(true);
    expect(canWrite(roles, ModuleRoles.farm)).toBe(false);
    expect(canWrite(roles, ModuleRoles.admin)).toBe(false);
  });

  it('module roles are confined to their module', () => {
    const roles = [Roles.FARM_MANAGER];
    expect(canRead(roles, ModuleRoles.farm)).toBe(true);
    expect(canWrite(roles, ModuleRoles.farm)).toBe(true);
    expect(canRead(roles, ModuleRoles.hatchery)).toBe(false);
    expect(canRead(roles, ModuleRoles.admin)).toBe(false);
    expect(canRead(roles, ModuleRoles.reporting)).toBe(true);
  });

  it('derives the user from the access token', () => {
    const user = userFromToken(token({ sub: 'u1', username: 'alin', name: 'Alin', roles: [{ role: 'SYSTEM_ADMIN' }, { role: 'SYSTEM_ADMIN' }] }));
    expect(user).toEqual({ id: 'u1', email: 'alin', displayName: 'Alin', roles: ['SYSTEM_ADMIN'] });
    expect(userFromToken('garbage')).toBeNull();
  });
});
