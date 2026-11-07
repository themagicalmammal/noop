import XCTest
@testable import StrandDesign

/// The dot-matrix glyph layout is a cross-platform byte-identical contract (twin: `DotNumberTest`,
/// Android). The same shared literal is pasted verbatim into both tests, so a drift in either
/// glyph table fails on both platforms at once.
final class DotMatrixTests: XCTestCase {
    /// `grid("26,580")` — 7 rows x 23 columns; each group of 4 columns is one glyph + gap.
    private static let expected: [[Int]] = [
        [1,1,1,0, 1,1,1,0, 0,0,0,0, 1,1,1,0, 1,1,1,0, 1,1,1],
        [0,0,1,0, 1,0,0,0, 0,0,0,0, 1,0,0,0, 1,0,1,0, 1,0,1],
        [1,1,1,0, 1,1,1,0, 0,0,0,0, 1,1,1,0, 1,0,1,0, 1,0,1],
        [1,1,0,0, 1,0,1,0, 0,0,0,0, 0,0,1,0, 1,1,1,0, 1,0,1],
        [1,0,0,0, 1,0,1,0, 0,0,0,0, 0,0,1,0, 1,0,1,0, 1,0,1],
        [1,0,0,0, 1,0,1,0, 1,1,1,0, 0,0,1,0, 1,0,1,0, 1,0,1],
        [1,1,1,0, 1,1,1,0, 1,1,1,0, 1,1,1,0, 1,1,1,0, 1,1,1],
    ]

    func testGridLayoutMatchesSharedLiteral() {
        XCTAssertEqual(DotMatrix.grid("26,580"), Self.expected)
    }

    func testEmptyTextYieldsEmptyRows() {
        let grid = DotMatrix.grid("")
        XCTAssertEqual(grid.count, DotMatrix.rows)
        XCTAssertTrue(grid.allSatisfy { $0.isEmpty })
    }

    func testUnknownCharactersRenderBlank() {
        let grid = DotMatrix.grid("?")
        XCTAssertEqual(grid.count, DotMatrix.rows)
        XCTAssertEqual(grid.first?.count, DotMatrix.glyphColumns)
        XCTAssertTrue(grid.allSatisfy { row in row.allSatisfy { $0 == 0 } })
    }
}
