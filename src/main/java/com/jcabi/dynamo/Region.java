/**
 * Copyright (c) 2012-2015, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Amazon DynamoDB region.
 *
 * <p>It is recommended to use {@link Region.Simple} in most cases.
 *
 * <p>You can use {@link #aws()} method to get access to Amazon DynamoDB
 * client directly.
 *
 * <p>Since version 0.9 it is strongly recommended to wrap your region
 * in {@link com.jcabi.dynamo.retry.ReRegion} before use, for example:
 *
 * <pre> Region region = new ReRegion(new Region.Simple(credentials));</pre>
 *
 * <p>After all operations with the region are finished, it can be optionally
 * shutdown invoking {@link AmazonDynamoDB#shutdown()}. Callers are not expected
 * to call it, but can if they want to explicitly release any open resources and
 * forcibly terminate all pending asynchronous service calls. Once a client has
 * been shutdown, it should not be used to make any more requests.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@Immutable
public interface Region {

    /**
     * Get DynamoDB client.
     * @return The client
     */
    @NotNull(message = "AWS DynamoDB client is never NULL")
    AmazonDynamoDB aws();

    /**
     * Get one table.
     * @param name Table name
     * @return Table
     */
    @NotNull(message = "table is never NULL")
    Table table(@NotNull(message = "table name can't be NULL") String name);

    /**
     * Simple region, basic implementation.
     */
    @Immutable
    @Loggable(Loggable.DEBUG)
    @ToString
    @EqualsAndHashCode(of = "credentials")
    final class Simple implements Region {
        /**
         * Credentials.
         */
        private final transient Credentials credentials;
        /**
         * Public ctor.
         * @param creds Credentials
         */
        public Simple(@NotNull final Credentials creds) {
            this.credentials = creds;
        }
        @Override
        @NotNull(message = "AWS client is never NULL")
        public AmazonDynamoDB aws() {
            return this.credentials.aws();
        }
        @Override
        @NotNull(message = "table is never NULL")
        public Table table(@NotNull(message = "table name can't be NULL")
            final String name) {
            return new AwsTable(this.credentials, this, name);
        }
    }

    /**
     * All tables have a prefix in front of their names.
     *
     * <p>The region has to be used in combination with another region,
     * for example {@link Region.Simple}:
     *
     * <pre>Region region = new Region.Prefixed(
     *   new Region.Simple(creds),
     *   "foo-"
     * );</pre>
     *
     * <p>Now, {@code region.table("test")} will return a {@link Table}
     * instance pointing to the Dynamo DB table named {@code "foo-test"}. Could
     * be a convenient mechanism when you have many tables for different
     * projects in the same region.
     */
    @Immutable
    @Loggable(Loggable.DEBUG)
    @ToString
    @EqualsAndHashCode(of = { "origin", "prefix" })
    final class Prefixed implements Region {
        /**
         * Original region.
         */
        private final transient Region origin;
        /**
         * Prefix to add.
         */
        private final transient String prefix;
        /**
         * Public ctor.
         * @param region Original region
         * @param pfx Prefix to add to all tables
         */
        public Prefixed(
            @NotNull(message = "region can't be NULL") final Region region,
            @NotNull(message = "prefix can't be NULL") final String pfx) {
            this.origin = region;
            this.prefix = pfx;
        }
        @Override
        @NotNull(message = "AmazonDynamoDB cannot be null")
        public AmazonDynamoDB aws() {
            return this.origin.aws();
        }
        @Override
        @NotNull(message = "Table cannot be null")
        public Table table(@NotNull final String name) {
            return this.origin.table(
                new StringBuilder(this.prefix).append(name).toString()
            );
        }
    }

}
