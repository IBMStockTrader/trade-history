/*
       Copyright 2025 Kyndryl, All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.bson.types.ObjectId;

import jakarta.json.bind.annotation.JsonbCreator;

import java.time.Instant;


/**
 * Transaction record
 * The annotations are not needed on any JAX-RS or other clients as they are specific for the
 * interacting only with the MongoDB driver
 */
public record Transaction(
    String owner,
    Integer shares,
    String symbol,
    Float notional,
    Float price,
    Float commission,
    @BsonId @BsonRepresentation(BsonType.OBJECT_ID) String _id,
    Instant when
) {
    
    @JsonbCreator
    public Transaction(String owner, Integer shares, String symbol, Float notional, Float price, Float commission, String _id, Instant when) {
        this.owner = owner;
        this.shares = shares;
        this.symbol = symbol;
        this.notional = notional;
        this.price = price;
        this.commission = commission;
        this._id = _id;
        this.when = when;
    }

    // This constructor is used when creating a new Transaction to be stored in MongoDB only. This is only needed in
    // the trade-history service. Remove for all other services
    public Transaction(String owner, Integer shares, String symbol, Float price, Float commission, Instant when) {
        this(owner, shares, symbol, (price * shares), price, commission, new ObjectId().toHexString(), when);
    }

}
