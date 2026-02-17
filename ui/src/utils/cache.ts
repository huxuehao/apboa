/**
 * 会话级别缓存
 */
const sessionCache = {
  set(key: string, value: string) {
    if (!sessionStorage) {
      return;
    }

    if (key != null && value != null) {
      sessionStorage.setItem(key, value);
    }
  },

  get(key: string) {
    if (!sessionStorage) {
      return;
    }

    if (key == null) {
      return null;
    }

    return sessionStorage.getItem(key);
  },

  setJSON(key: string, value: object) {
    if (value != null) {
      this.set(key, JSON.stringify(value));
    }
  },

  getJSON(key: string, def: object = {}) {
    const value = this.get(key);
    if (value != null) {
      return JSON.parse(value);
    }
    return def;
  },

  remove(key: string) {
    sessionStorage.removeItem(key);
  },
};

/**
 * 本地持久缓存
 */
const localCache = {
  set(key: string, value: string) {
    if (!localStorage) {
      return;
    }

    if (key != null && value != null) {
      localStorage.setItem(key, value);
    }
  },

  get(key: string) {
    if (!localStorage) {
      console.error();

      return;
    }

    if (key == null) {
      return null;
    }

    return localStorage.getItem(key);
  },

  setJSON(key: string, value: object) {
    if (value != null) {
      this.set(key, JSON.stringify(value));
    }
  },

  getJSON(key: string, def: object = {}) {
    const value = this.get(key);
    if (value != null) {
      return JSON.parse(value);
    }
    return def;
  },

  remove(key: string) {
    localStorage.removeItem(key);
  },
};

export default {
  // 回话级别的缓存
  session: sessionCache,
  // 本地缓存
  local: localCache,
};
