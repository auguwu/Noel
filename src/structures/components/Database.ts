/**
 * Copyright (c) 2020-2021 August
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import ComponentImpl from '../Component';
import Component from '../decorators/Component';
import Logger from '../Logger';
import Config from './Config';
import pg from 'pg';

interface User {
  birthday: string | null;
  cutie: boolean;
  id: string;
}

export default class DatabaseComponent implements ComponentImpl {
  public priority = 1;
  private logger = Logger.get();
  public name = 'database';

  @Component private config!: Config;

  #client!: pg.PoolClient;
  #pool!: pg.Pool;

  async load() {
    this.logger.log('Connecting to PostgreSQL...');

    const dbUrl = this.config.getProperty('DATABASE_CONNECTION_URL')!;
    this.#pool = new pg.Pool({ connectionString: dbUrl });

    this.#client = await this.#pool.connect();
    this.logger.log('Connected to PostgreSQL!');
  }

  query<T extends object = {}>(sql: string): Promise<T | null> {
    return this.#client.query<T>(sql)
      .then(result => result?.rows[0] ?? null);
  }

  getUser(id: string) {
    return this.query<User>(`SELECT * FROM users WHERE id = '${id}';`);
  }

  createUser(id: string) {
    return this.query<any>(`INSERT INTO users (birthday, cutie, id) VALUES (NULL, false, '${id}');`);
  }

  deleteUser(id: string) {
    return this.query<any>(`DELETE FROM users WHERE id = '${id}';`);
  }
}
