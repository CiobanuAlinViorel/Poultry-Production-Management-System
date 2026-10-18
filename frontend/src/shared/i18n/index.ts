import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import roCommon from './locales/ro/common.json';
import roErrors from './locales/ro/errors.json';
import roIdentity from './locales/ro/identity.json';
import roAdmin from './locales/ro/admin.json';
import roFarm from './locales/ro/farm.json';
import roSites from './locales/ro/sites.json';
import enCommon from './locales/en/common.json';
import enErrors from './locales/en/errors.json';
import enIdentity from './locales/en/identity.json';
import enAdmin from './locales/en/admin.json';
import enFarm from './locales/en/farm.json';
import enSites from './locales/en/sites.json';

void i18n.use(LanguageDetector).use(initReactI18next).init({
  resources: {
    ro: { common: roCommon, errors: roErrors, identity: roIdentity, admin: roAdmin, farm: roFarm, sites: roSites },
    en: { common: enCommon, errors: enErrors, identity: enIdentity, admin: enAdmin, farm: enFarm, sites: enSites },
  },
  fallbackLng: 'ro',
  supportedLngs: ['ro', 'en'],
  defaultNS: 'common',
  ns: ['common', 'errors', 'identity', 'admin', 'farm', 'sites'],
  interpolation: { escapeValue: false },
  detection: { order: ['localStorage', 'navigator'], caches: ['localStorage'] },
});

export default i18n;
