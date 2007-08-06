/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package edu.byu.cs.adbcj.mysql;

import java.util.Set;

import edu.byu.cs.adbcj.support.DefaultField;

public class MysqlField extends DefaultField {

	private final MysqlType mysqlType;
	private final MysqlCharacterSet characterSet;
	private final long length;
	private final Set<FieldFlag> flags;
	private final long fieldDefault;
	
	public MysqlField(
			String catalogName,
			String schemaName,
			String tableLabel,
			String tableName,
			MysqlType mysqlType,
			String columnLabel,
			String columnName,
			int precision,
			int decimals,
			MysqlCharacterSet characterSet,
			long length,
			Set<FieldFlag> flags,
			long fieldDefault) {
		
		super(
				catalogName,
				schemaName,
				tableLabel,
				tableName,
				mysqlType.getType(),
				columnLabel,
				columnName,
				precision,
				decimals,
				flags.contains(FieldFlag.AUTO_INCREMENT),
				mysqlType.isCaseSensitive(),
				mysqlType.isCurrency(),
				false,
				!flags.contains(FieldFlag.NOT_NULL),
				true, // TODO figure this out
				false, // TODO figure this out
				!flags.contains(FieldFlag.UNSIGNED),
				false, // TODO figure this out
				mysqlType.getClassName());
		this.mysqlType = mysqlType;
		this.characterSet = characterSet;
		this.length = length;
		this.flags = flags;
		this.fieldDefault = fieldDefault;
	}
	
	public MysqlType getMysqlType() {
		return mysqlType;
	}

	public MysqlCharacterSet getCharacterSet() {
		return characterSet;
	}

	public long getLength() {
		return length;
	}

	public Set<FieldFlag> getFlags() {
		return flags;
	}

	public long getFieldDefault() {
		return fieldDefault;
	}

}
