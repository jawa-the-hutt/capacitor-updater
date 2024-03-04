/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import Foundation

@objc public class DecryptStrategy: NSObject, Decodable, Encodable {

    private let type: String?
    private let key: String?

    convenience init(type: String, key: String?) {
        self.init(type: type, key: key)
    }

    init(type: String?, key: String?) {
        self.type = type
        self.key = key
    }

    enum CodingKeys: String, CodingKey {
        case type, key
    }

    public func getType() -> String {
        return self.type
    }

    public func getKey() -> String? {
        return self.key
    }

    public func toJSON() -> [String: String] {
        return [
            "type": self.getType(),
            "key": self.getKey() ?? ""
        ]
    }

    public static func == (lhs: DecryptStrategy, rhs: DecryptStrategy) -> Bool {
        return lhs.getType() == rhs.getType() && lhs.getKey() == rhs.getKey()
    }

    public func toString() -> String {
        return "{ \"type\": \"\(self.getType())\", \"key\": \"\(self.getKey() ?? "")\"}"
    }

}
